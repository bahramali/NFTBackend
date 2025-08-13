package se.hydroleaf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.hydroleaf.dto.AggregatedHistoryResponse;
import se.hydroleaf.dto.AggregatedSensorData;
import se.hydroleaf.dto.TimestampValue;
import se.hydroleaf.model.Device;
import se.hydroleaf.model.OxygenPumpStatus;
import se.hydroleaf.model.SensorData;
import se.hydroleaf.model.SensorHealthItem;
import se.hydroleaf.model.SensorRecord;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.OxygenPumpStatusRepository;
import se.hydroleaf.repository.SensorRecordRepository;
import se.hydroleaf.util.InstantUtil;

import java.time.Instant;
import java.util.*;

/**
 * RecordService aligned with the "Option 1" data model:
 * - Device PK = composite_id (String)
 * - SensorRecord has FK to Device.composite_id
 * - SensorRecord cascades SensorData and SensorHealthItem
 *
 * Responsibilities:
 *  1) Persist a sensor record for a given device from a JSON payload.
 *  2) Read aggregated history by time bucket via SensorAggregationReader abstraction.
 *
 * Assumptions:
 *  - Repositories exist: DeviceRepository, SensorRecordRepository, OxygenPumpStatusRepository.
 *  - SensorRecord entity uses CascadeType.ALL for values (SensorData) and health items (SensorHealthItem).
 *  - There is a repository/adapter implementing SensorAggregationReader (native/JPQL).
 */
@Service
public class RecordService {

    private final ObjectMapper objectMapper;
    private final DeviceRepository deviceRepository;
    private final SensorRecordRepository recordRepository;
    private final OxygenPumpStatusRepository pumpRepository;
    private final SensorAggregationReader aggregationReader; // thin facade over custom repo/projection

    public RecordService(
            ObjectMapper objectMapper,
            DeviceRepository deviceRepository,
            SensorRecordRepository recordRepository,
            OxygenPumpStatusRepository pumpRepository,
            SensorAggregationReader aggregationReader
    ) {
        this.objectMapper = objectMapper;
        this.deviceRepository = deviceRepository;
        this.recordRepository = recordRepository;
        this.pumpRepository = pumpRepository;
        this.aggregationReader = aggregationReader;
    }

    /**
     * Persist a single record for a device.
     * JSON structure is intentionally flexible. Example:
     *  {
     *    "timestamp": "2025-08-14T10:20:00Z",  // optional, default now()
     *    "values": {
     *       "light":       {"value": 550.2, "unit": "lx",    "source":"raw"},
     *       "temperature": {"value": 23.8,  "unit": "°C"},
     *       "humidity":    {"value": 42.1,  "unit": "%"},
     *       "ph":          {"value": 6.1},
     *       "ec":          {"value": 1.58,  "unit": "mS/cm"},
     *       "do":          {"value": 4.4,   "unit": "mg/L"}
     *    },
     *    "health": {
     *       "temperature": true,
     *       "ph": true
     *    },
     *    "air_pump": true
     *  }
     *
     * Unknown keys are ignored. Missing sections are skipped.
     */
    @Transactional
    public void saveRecord(String compositeId, JsonNode json) {
        Objects.requireNonNull(compositeId, "compositeId is required");

        final Device device = deviceRepository.findById(compositeId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown device composite_id: " + compositeId));

        final Instant ts = parseTimestamp(json.path("timestamp")).orElseGet(Instant::now);

        SensorRecord record = new SensorRecord();
        record.setDevice(device);
        record.setTimestamp(ts);

        // Parse numeric values
        JsonNode values = json.path("values");
        if (values.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = values.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                String name = e.getKey();      // e.g., "light","temperature","humidity","ph","ec","do"
                JsonNode v = e.getValue();

                Double num = readDouble(v.path("value")).orElse(null);
                if (num == null) continue;     // skip invalid entries

                SensorData d = new SensorData();
                d.setRecord(record);
                d.setSensorName(name);
                d.setValueType("number");
                d.setValue(num);
                if (v.hasNonNull("unit"))   d.setUnit(v.get("unit").asText());
                if (v.hasNonNull("source")) d.setSource(v.get("source").asText());

                record.getValues().add(d);
            }
        }

        // Parse health booleans
        JsonNode health = json.path("health");
        if (health.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = health.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                String type = e.getKey();
                JsonNode v = e.getValue();
                if (!v.isBoolean()) continue;

                SensorHealthItem h = new SensorHealthItem();
                h.setRecord(record);
                h.setSensorType(type);
                h.setStatus(v.asBoolean());

                record.getHealthItems().add(h);
            }
        }

        // Persist the record (cascades values + health)
        recordRepository.save(record);

        // Optional: air pump status
        if (json.has("air_pump") && json.get("air_pump").isBoolean()) {
            OxygenPumpStatus ps = new OxygenPumpStatus();
            ps.setDevice(device);
            ps.setTimestamp(ts);
            ps.setStatus(json.get("air_pump").asBoolean());
            pumpRepository.save(ps);
        }
    }

    /**
     * Read aggregated history for a device within [from, to], bucketed by granularity.
     * Delegates the heavy lifting to SensorAggregationReader (custom repo/projection).
     */
    @Transactional(readOnly = true)
    public AggregatedHistoryResponse aggregatedHistory(
            String compositeId,
            Instant from,
            Instant to,
            String bucket // e.g., "5m","1h","1d"
    ) {
        if (from == null || to == null) throw new IllegalArgumentException("from/to are required");
        if (!deviceRepository.existsById(compositeId)) {
            throw new IllegalArgumentException("Unknown device composite_id: " + compositeId);
        }

        Instant bucketFrom = InstantUtil.truncateToBucket(from, bucket);
        Instant bucketTo   = InstantUtil.truncateToBucket(to, bucket);

        List<SensorAggregateResult> results =
                aggregationReader.aggregate(compositeId, bucketFrom, bucketTo, bucket);

        // Collate by (sensorName|valueType|unit)
        Map<String, AggregatedSensorData> map = new LinkedHashMap<>();
        for (SensorAggregateResult r : results) {
            String key = r.getSensorName() + "|" + r.getValueType() + "|" + r.getUnit();
            AggregatedSensorData agg = map.computeIfAbsent(key, k ->
                    new AggregatedSensorData(r.getSensorName(), r.getValueType(), r.getUnit(), new ArrayList<>())
            );
            agg.data().add(new TimestampValue(r.getBucketTime(), r.getAvgValue()));
        }

        return new AggregatedHistoryResponse(bucketFrom, bucketTo, new ArrayList<>(map.values()));
    }

    // ---------- helpers ----------

    private Optional<Instant> parseTimestamp(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return Optional.empty();
        if (node.isNumber()) return Optional.of(Instant.ofEpochMilli(node.asLong()));
        if (node.isTextual()) {
            try { return Optional.of(Instant.parse(node.asText())); } catch (Exception ignore) {}
        }
        return Optional.empty();
    }

    private Optional<Double> readDouble(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return Optional.empty();
        if (node.isNumber()) return Optional.of(node.asDouble());
        if (node.isTextual()) {
            try { return Optional.of(Double.parseDouble(node.asText())); } catch (Exception ignore) {}
        }
        return Optional.empty();
    }

    /**
     * Abstraction so this service does not depend on a specific repository implementation.
     * Implement this with your Spring Data repository (native query / JPQL) that returns projections.
     */
    public interface SensorAggregationReader {
        List<SensorAggregateResult> aggregate(String compositeId, Instant from, Instant to, String bucket);
    }

    /**
     * Minimal projection interface the repository should return for aggregation.
     */
    public interface SensorAggregateResult {
        String getSensorName();
        String getValueType();
        String getUnit();
        Instant getBucketTime();
        Double getAvgValue();
    }
}
