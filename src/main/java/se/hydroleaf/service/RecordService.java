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
import se.hydroleaf.model.LatestSensorValue;
import se.hydroleaf.model.SensorValueHistory;
import se.hydroleaf.repository.ActuatorStatusRepository;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.LatestSensorValueRepository;
import se.hydroleaf.repository.SensorValueHistoryRepository;
import se.hydroleaf.util.InstantUtil;

import java.time.Instant;
import java.util.*;

/**
 * Service responsible for processing incoming sensor records and storing them in
 * the new simplified schema.
 *
 * <p>Sensor readings are written directly to {@code sensor_value_history} and
 * the latest value per device and type is mirrored in the
 * {@code latest_sensor_value} table. Aggregated history queries are delegated to
 * {@link SensorAggregationReader} implementations.</p>
 */
@Service
public class RecordService {

    private final DeviceRepository deviceRepository;
    private final SensorValueHistoryRepository sensorValueHistoryRepository;
    private final ActuatorStatusRepository actuatorStatusRepository;
    private final SensorAggregationReader aggregationReader; // thin facade over custom repo/projection
    private final LatestSensorValueRepository latestSensorValueRepository;

    public RecordService(
            DeviceRepository deviceRepository,
            SensorValueHistoryRepository sensorValueHistoryRepository,
            ActuatorStatusRepository actuatorStatusRepository,
            SensorAggregationReader aggregationReader,
            LatestSensorValueRepository latestSensorValueRepository
    ) {
        this.deviceRepository = deviceRepository;
        this.sensorValueHistoryRepository = sensorValueHistoryRepository;
        this.actuatorStatusRepository = actuatorStatusRepository;
        this.aggregationReader = aggregationReader;
        this.latestSensorValueRepository = latestSensorValueRepository;
    }

    @Transactional
    public void saveRecord(String compositeId, JsonNode json) {
        Objects.requireNonNull(compositeId, "compositeId is required");

        final Device device = deviceRepository.findById(compositeId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown device composite_id: " + compositeId));

        final Instant ts = parseTimestamp(json.path("timestamp")).orElseGet(Instant::now);

        // Parse sensor readings from sensors array and persist directly to history
        JsonNode sensors = json.path("sensors");
        if (sensors.isArray()) {
            Set<String> seenTypes = new HashSet<>();
            for (JsonNode s : sensors) {
                String sensorType = s.path("sensorType").asText(null);
                if (sensorType == null || sensorType.isBlank() || !seenTypes.add(sensorType)) {
                    continue;
                }

                JsonNode valueNode = s.path("value");
                Double num;
                if (valueNode.isObject()) {
                    num = readDouble(valueNode.path("value")).orElse(null);
                } else {
                    num = readDouble(valueNode).orElse(null);
                }
                if (num == null) continue;

                SensorValueHistory history = SensorValueHistory.builder()
                        .compositeId(compositeId)
                        .sensorType(sensorType)
                        .sensorValue(num)
                        .valueTime(ts)
                        .build();
                sensorValueHistoryRepository.save(history);

                LatestSensorValue lsv = latestSensorValueRepository
                        .findByDevice_CompositeIdAndSensorType(compositeId, sensorType)
                        .orElseGet(() -> {
                            LatestSensorValue n = new LatestSensorValue();
                            n.setDevice(device);
                            n.setSensorType(sensorType);
                            return n;
                        });
                lsv.setValue(num);
                if (valueNode.isObject() && valueNode.hasNonNull("unit")) {
                    lsv.setUnit(valueNode.get("unit").asText());
                } else if (s.hasNonNull("unit")) {
                    lsv.setUnit(s.get("unit").asText());
                }
                lsv.setValueTime(ts);
                latestSensorValueRepository.save(lsv);
            }
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

    public interface SensorAggregationReader {
        List<SensorAggregateResult> aggregate(String compositeId, Instant from, Instant to, String bucket, String sensorType);
    }

    public interface SensorAggregateResult {
        String getSensorType();
        String getUnit();
        Instant getBucketTime();
        Double getAvgValue();
    }
}
