package se.hydroleaf.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.hydroleaf.repository.dto.history.AggregatedHistoryResponse;
import se.hydroleaf.repository.dto.history.AggregatedSensorData;
import se.hydroleaf.repository.dto.history.TimestampValue;
import se.hydroleaf.model.ActuatorStatus;
import se.hydroleaf.model.Device;
import se.hydroleaf.model.DeviceGroup;
import se.hydroleaf.model.LatestSensorValue;
import se.hydroleaf.repository.ActuatorStatusRepository;
import se.hydroleaf.repository.DeviceGroupRepository;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.LatestSensorValueRepository;
import se.hydroleaf.util.InstantUtil;

import java.time.Instant;
import java.util.*;

/**
 * Service responsible for processing incoming sensor records and storing them in
 * the new simplified schema.
 *
 * <p>Sensor readings are buffered for periodic aggregation into
 * {@code sensor_value_history} while the latest value per device and type is
 * mirrored in the {@code latest_sensor_value} table immediately. Aggregated
 * history queries are delegated to {@link SensorAggregationReader}
 * implementations.</p>
 */
@Service
public class RecordService {

    private static final Logger log = LoggerFactory.getLogger(RecordService.class);

    private final DeviceRepository deviceRepository;
    private final DeviceGroupRepository deviceGroupRepository;
    private final ActuatorStatusRepository actuatorStatusRepository;
    private final SensorAggregationReader aggregationReader; // thin facade over custom repo/projection
    private final LatestSensorValueRepository latestSensorValueRepository;
    private final SensorValueBuffer sensorValueBuffer;

    public RecordService(
            DeviceRepository deviceRepository,
            DeviceGroupRepository deviceGroupRepository,
            ActuatorStatusRepository actuatorStatusRepository,
            SensorAggregationReader aggregationReader,
            LatestSensorValueRepository latestSensorValueRepository,
            SensorValueBuffer sensorValueBuffer
    ) {
        this.deviceRepository = deviceRepository;
        this.deviceGroupRepository = deviceGroupRepository;
        this.actuatorStatusRepository = actuatorStatusRepository;
        this.aggregationReader = aggregationReader;
        this.latestSensorValueRepository = latestSensorValueRepository;
        this.sensorValueBuffer = sensorValueBuffer;
    }

    @Transactional
    public void saveRecord(String compositeId, JsonNode json) {
        Objects.requireNonNull(compositeId, "compositeId is required");

        final Device device = deviceRepository.findById(compositeId)
                .orElseGet(() -> autoRegisterDevice(compositeId));

        final Instant ts = parseTimestamp(json.path("timestamp")).orElseGet(Instant::now);

        // Parse sensor readings from sensors array and buffer them for aggregation
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

                sensorValueBuffer.add(compositeId, sensorType, num, ts);

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

    private Device autoRegisterDevice(String compositeId) {
        String[] parts = compositeId.split("-", 3);
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid compositeId: " + compositeId);
        }
        DeviceGroup group = deviceGroupRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> deviceGroupRepository.save(DeviceGroup.builder().mqttTopic("default").build()));

        Device device = new Device();
        device.setCompositeId(compositeId);
        device.setSystem(parts[0]);
        device.setLayer(parts[1]);
        device.setDeviceId(parts[2]);
        device.setGroup(group);
        deviceRepository.save(device);
        log.info("Auto-registered unknown device {}", compositeId);
        return device;
    }

    @Transactional(readOnly = true)
    public AggregatedHistoryResponse aggregatedHistory(
            String compositeId,
            Instant from,
            Instant to,
            String bucket, // e.g., "5m","1h","1d"
            List<String> sensorTypes // optional filter
    ) {
        return aggregatedHistory(compositeId, from, to, bucket, sensorTypes, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public AggregatedHistoryResponse aggregatedHistory(
            String compositeId,
            Instant from,
            Instant to,
            String bucket,
            List<String> sensorTypes,
            Integer bucketLimit,
            Integer bucketOffset,
            Integer sensorLimit,
            Integer sensorOffset
    ) {
        if (from == null || to == null) throw new IllegalArgumentException("from/to are required");
        if (!deviceRepository.existsById(compositeId)) {
            throw new IllegalArgumentException("Unknown device composite_id: " + compositeId);
        }

        Instant bucketFrom = InstantUtil.truncateToBucket(from, bucket);
        Instant bucketTo   = InstantUtil.truncateToBucket(to, bucket);

        List<SensorAggregateResult> results = new ArrayList<>();
        if (sensorTypes == null || sensorTypes.isEmpty()) {
            results.addAll(aggregationReader.aggregate(compositeId, bucketFrom, bucketTo, bucket, null));
        } else {
            for (String st : sensorTypes) {
                results.addAll(aggregationReader.aggregate(compositeId, bucketFrom, bucketTo, bucket, st));
            }
        }

        // Collate by (sensorType|unit)
        Map<String, AggregatedSensorData> map = new LinkedHashMap<>();
        for (SensorAggregateResult r : results) {
            String key = r.getSensorType() + "|" + r.getUnit();
            AggregatedSensorData agg = map.computeIfAbsent(key, k ->
                    new AggregatedSensorData(r.getSensorType(), r.getUnit(), new ArrayList<>())
            );
            agg.data().add(new TimestampValue(r.getBucketTime(), r.getAvgValue()));
        }

        List<AggregatedSensorData> sensors = new ArrayList<>();
        for (AggregatedSensorData agg : map.values()) {
            List<TimestampValue> data = agg.data();
            int bFrom = bucketOffset == null ? 0 : Math.max(0, bucketOffset);
            int bTo = bucketLimit == null ? data.size() : Math.min(data.size(), bFrom + bucketLimit);
            bFrom = Math.min(bFrom, data.size());
            bTo = Math.min(bTo, data.size());
            List<TimestampValue> slice = new ArrayList<>(data.subList(bFrom, bTo));
            sensors.add(new AggregatedSensorData(agg.sensorType(), agg.unit(), slice));
        }

        int sFrom = sensorOffset == null ? 0 : Math.max(0, sensorOffset);
        int sTo = sensorLimit == null ? sensors.size() : Math.min(sensors.size(), sFrom + sensorLimit);
        sFrom = Math.min(sFrom, sensors.size());
        sTo = Math.min(sTo, sensors.size());
        List<AggregatedSensorData> finalSensors = new ArrayList<>(sensors.subList(sFrom, sTo));

        return new AggregatedHistoryResponse(bucketFrom, bucketTo, finalSensors);
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
