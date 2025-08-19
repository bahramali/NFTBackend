package se.hydroleaf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.hydroleaf.dto.history.AggregatedHistoryResponse;
import se.hydroleaf.dto.history.AggregatedSensorData;
import se.hydroleaf.dto.history.TimestampValue;
import se.hydroleaf.model.ActuatorStatus;
import se.hydroleaf.model.Device;
import se.hydroleaf.model.SensorData;
import se.hydroleaf.model.SensorHealthItem;
import se.hydroleaf.model.SensorRecord;
import se.hydroleaf.repository.ActuatorStatusRepository;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.SensorRecordRepository;
import se.hydroleaf.util.InstantUtil;

import jakarta.persistence.EntityManager;

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
 *  - Repositories exist: DeviceRepository, SensorRecordRepository, ActuatorStatusRepository.
 *  - SensorRecord entity uses CascadeType.ALL for values (SensorData) and health items (SensorHealthItem).
 *  - There is a repository/adapter implementing SensorAggregationReader (native/JPQL).
 */
@Service
public class RecordService {

    private final ObjectMapper objectMapper;
    private final DeviceRepository deviceRepository;
    private final SensorRecordRepository recordRepository;
    private final ActuatorStatusRepository actuatorStatusRepository;
    private final SensorAggregationReader aggregationReader; // thin facade over custom repo/projection
    private final EntityManager entityManager;

    public RecordService(
            ObjectMapper objectMapper,
            DeviceRepository deviceRepository,
            SensorRecordRepository recordRepository,
            ActuatorStatusRepository actuatorStatusRepository,
            SensorAggregationReader aggregationReader,
            EntityManager entityManager
    ) {
        this.objectMapper = objectMapper;
        this.deviceRepository = deviceRepository;
        this.recordRepository = recordRepository;
        this.actuatorStatusRepository = actuatorStatusRepository;
        this.aggregationReader = aggregationReader;
        this.entityManager = entityManager;
    }

    /**
     * Persist a single record for a device.
     * JSON structure is intentionally flexible. Example:
     *  {
     *    "timestamp": "2025-08-14T10:20:00Z",  // optional, default now()
     *    "values": {
     *       "light":       {"value": 550.2, "unit": "lx"},
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

        // Parse sensor readings from sensors array
        JsonNode sensors = json.path("sensors");
        Map<String, String> sensorNameToType = new HashMap<>();
        Set<String> seenTypes = new HashSet<>();
        if (sensors.isArray()) {
            for (JsonNode s : sensors) {
                String sensorType = s.path("sensorType").asText(null);
                if (sensorType == null || sensorType.isBlank() || !seenTypes.add(sensorType)) {
                    // skip null, empty or duplicate sensor types
                    continue;
                }
                String sensorName = s.path("sensorName").asText(null);
                if (sensorName != null) {
                    sensorNameToType.put(sensorName, sensorType);
                }

                // support value as either a primitive or an object with {value,unit}
                JsonNode valueNode = s.path("value");
                Double num;
                String unit = null;
                if (valueNode.isObject()) {
                    num = readDouble(valueNode.path("value")).orElse(null);
                    if (valueNode.hasNonNull("unit")) unit = valueNode.get("unit").asText();
                } else {
                    num = readDouble(valueNode).orElse(null);
                    if (s.hasNonNull("unit")) unit = s.get("unit").asText();
                }
                if (num == null) continue; // skip invalid

                SensorData d = new SensorData();
                d.setRecord(record);
                d.setSensorType(sensorType); // logical type
                d.setValue(num);
                if (unit != null) d.setUnit(unit);

                record.getValues().add(d);
            }
        }

        // Parse health booleans keyed by sensorName
        JsonNode health = json.path("health");
        if (health.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = health.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                String sensorName = e.getKey();
                JsonNode v = e.getValue();
                if (!v.isBoolean()) continue;
                String sensorType = sensorNameToType.get(sensorName);
                if (sensorType == null) continue;

                SensorHealthItem h = new SensorHealthItem();
                h.setRecord(record);
                h.setSensorType(sensorType);
                h.setStatus(v.asBoolean());

                record.getHealthItems().add(h);
            }
        }

        // Persist the record (cascades values + health)
        recordRepository.save(record);

        // Maintain latest_sensor_value materialized table
        for (SensorData d : record.getValues()) {
            entityManager.createNativeQuery(
                    "INSERT INTO latest_sensor_value (composite_id, sensor_type, sensor_value, unit, value_time) " +
                            "VALUES (?1, ?2, ?3, ?4, ?5) " +
                            "ON CONFLICT (composite_id, sensor_type) DO UPDATE " +
                            "SET sensor_value = EXCLUDED.sensor_value, unit = EXCLUDED.unit, value_time = EXCLUDED.value_time")
                    .setParameter(1, compositeId)
                    .setParameter(2, d.getSensorType())
                    .setParameter(3, d.getValue())
                    .setParameter(4, d.getUnit())
                    .setParameter(5, ts)
                    .executeUpdate();
        }

        // Optional controllers array for actuator statuses
        JsonNode controllers = json.path("controllers");
        if (controllers.isArray()) {
            List<ActuatorStatus> statuses = new ArrayList<>();
            for (JsonNode c : controllers) {
                String type = c.path("name").asText(null);
                if (type == null) continue;
                JsonNode stateNode = c.path("state");
                if (!stateNode.isBoolean()) continue;
                ActuatorStatus as = new ActuatorStatus();
                as.setDevice(device);
                as.setTimestamp(parseTimestamp(c.path("timestamp")).orElse(ts));
                as.setActuatorType(type);
                as.setState(stateNode.asBoolean());
                statuses.add(as);
            }
            if (!statuses.isEmpty()) {
                actuatorStatusRepository.saveAll(statuses);
            }
        }
    }

    /**
     * Read aggregated history for a device within [from, to], bucketed by granularity.
     * Optionally filters to a single sensor type to minimize scanned rows.
     * Delegates the heavy lifting to SensorAggregationReader (custom repo/projection).
     */
    @Transactional(readOnly = true)
    public AggregatedHistoryResponse aggregatedHistory(
            String compositeId,
            Instant from,
            Instant to,
            String bucket, // e.g., "5m","1h","1d"
            String sensorType // optional filter
    ) {
        if (from == null || to == null) throw new IllegalArgumentException("from/to are required");
        if (!deviceRepository.existsById(compositeId)) {
            throw new IllegalArgumentException("Unknown device composite_id: " + compositeId);
        }

        Instant bucketFrom = InstantUtil.truncateToBucket(from, bucket);
        Instant bucketTo   = InstantUtil.truncateToBucket(to, bucket);

        List<SensorAggregateResult> results =
                aggregationReader.aggregate(compositeId, bucketFrom, bucketTo, bucket, sensorType);

        // Collate by (sensorType|unit)
        Map<String, AggregatedSensorData> map = new LinkedHashMap<>();
        for (SensorAggregateResult r : results) {
            String key = r.getSensorType() + "|" + r.getUnit();
            AggregatedSensorData agg = map.computeIfAbsent(key, k ->
                    new AggregatedSensorData(r.getSensorType(), r.getUnit(), new ArrayList<>())
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
        List<SensorAggregateResult> aggregate(String compositeId, Instant from, Instant to, String bucket, String sensorType);
    }

    /**
     * Minimal projection interface the repository should return for aggregation.
     */
    public interface SensorAggregateResult {
        String getSensorType();
        String getUnit();
        Instant getBucketTime();
        Double getAvgValue();
    }
}
