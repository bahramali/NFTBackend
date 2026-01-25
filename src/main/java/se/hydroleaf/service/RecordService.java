package se.hydroleaf.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.hydroleaf.mqtt.MqttTopicParser;
import se.hydroleaf.repository.dto.history.AggregatedHistoryResponse;
import se.hydroleaf.repository.dto.history.AggregatedSensorData;
import se.hydroleaf.repository.dto.history.TimestampValue;
import se.hydroleaf.model.ActuatorStatus;
import se.hydroleaf.model.Device;
import se.hydroleaf.model.LatestSensorValue;
import se.hydroleaf.model.TopicName;
import se.hydroleaf.repository.ActuatorStatusRepository;
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
    private final ActuatorStatusRepository actuatorStatusRepository;
    private final SensorAggregationReader aggregationReader; // thin facade over custom repo/projection
    private final LatestSensorValueRepository latestSensorValueRepository;
    private final SensorValueBuffer sensorValueBuffer;

    public RecordService(
            DeviceRepository deviceRepository,
            ActuatorStatusRepository actuatorStatusRepository,
            SensorAggregationReader aggregationReader,
            LatestSensorValueRepository latestSensorValueRepository,
            SensorValueBuffer sensorValueBuffer
    ) {
        this.deviceRepository = deviceRepository;
        this.actuatorStatusRepository = actuatorStatusRepository;
        this.aggregationReader = aggregationReader;
        this.latestSensorValueRepository = latestSensorValueRepository;
        this.sensorValueBuffer = sensorValueBuffer;
    }

    @Transactional
    public void saveRecord(String compositeId, JsonNode json, TopicName topic) {
        saveRecord(compositeId, json, topic, null, null);
    }

    @Transactional
    public void saveRecord(String compositeId, JsonNode json, TopicName topic, String mqttTopic,
                           MqttTopicParser.ParsedTopic parsedTopic) {
        Objects.requireNonNull(compositeId, "compositeId is required");

        String normalizedId = normalizeCompositeId(compositeId);

        final Device device = deviceRepository.findById(normalizedId)
                .orElseGet(() -> autoRegisterDevice(normalizedId, topic));

        final Instant ts = parseTimestamp(json.path("timestamp")).orElseGet(Instant::now);

        String messageKind = parsedTopic != null ? parsedTopic.kind() : readText(json, "kind");
        boolean isTelemetry = messageKind == null || "telemetry".equalsIgnoreCase(messageKind);
        String deviceId = parsedTopic != null ? parsedTopic.deviceId() : readText(json, "deviceId");
        if (deviceId == null) {
            deviceId = device.getDeviceId();
        }

        boolean storedMetric = false;
        if (isTelemetry) {
            storedMetric |= storeNumericMetric(json.get("lux"), normalizedId, device, "lux", "lux", ts);
            storedMetric |= storeNumericMetric(json.get("rh_pct"), normalizedId, device, "rh_pct", "%", ts);
            storedMetric |= storeNumericMetric(json.get("co2_ppm"), normalizedId, device, "co2_ppm", "ppm", ts);

            storedMetric |= storeNumericMetric(json.get("layer_temp_c"), normalizedId, device,
                    "layer_temp_c", "C", ts);
            storedMetric |= storeNumericMetric(json.get("air_temp_c"), normalizedId, device,
                    "air_temp_c", "C", ts);
            storedMetric |= storeNumericMetric(json.get("solution_temp_c"), normalizedId, device,
                    "solution_temp_c", "C", ts);

            JsonNode counts = json.get("as7343_counts");
            if (counts != null && counts.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = counts.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    String key = entry.getKey();
                    if (key == null || key.isBlank()) {
                        continue;
                    }
                    String sensorType = "as7343_counts_" + key;
                    storedMetric |= storeNumericMetric(entry.getValue(), normalizedId, device,
                            sensorType, "counts", ts);
                }
            }
        }

        if (isTelemetry && !storedMetric) {
            String topicLabel = mqttTopic != null ? mqttTopic : (topic != null ? topic.name() : "unknown");
            log.warn("Telemetry payload contains no supported metrics (topic={}, deviceId={})", topicLabel, deviceId);
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

    private boolean storeNumericMetric(JsonNode valueNode,
                                       String compositeId,
                                       Device device,
                                       String sensorType,
                                       String unit,
                                       Instant ts) {
        Double num = readDouble(valueNode).orElse(null);
        if (num == null) {
            return false;
        }

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
        lsv.setUnit(unit);
        lsv.setValueTime(ts);
        latestSensorValueRepository.save(lsv);
        return true;
    }

    private String readText(JsonNode node, String... fieldNames) {
        if (node == null || fieldNames == null) {
            return null;
        }

        for (String fieldName : fieldNames) {
            if (fieldName == null) {
                continue;
            }
            JsonNode field = node.get(fieldName);
            if (field != null && field.isTextual()) {
                return field.asText();
            }
        }

        return null;
    }

    private Device autoRegisterDevice(String compositeId, TopicName topic) {
        String[] parts = compositeId.split("-", 4);
        if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid compositeId: " + compositeId);
        }
        Device device = new Device();
        device.setCompositeId(compositeId);
        device.setSystem(parts[0]);
        device.setRack(parts[1]);
        device.setLayer(parts[2]);
        device.setDeviceId(parts[3]);
        device.setTopic(topic != null ? topic : TopicName.growSensors);
        deviceRepository.save(device);
        log.info("Auto-registered unknown device {}", compositeId);
        return device;
    }

    private String normalizeCompositeId(String compositeId) {
        String[] parts = compositeId.split("-", 4);
        if (parts.length == 4) {
            return compositeId;
        }
        if (parts.length == 3) {
            return String.format("%s-UNKNOWN-%s-%s", parts[0], parts[1], parts[2]);
        }
        throw new IllegalArgumentException("Invalid compositeId: " + compositeId);
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

        long bucketSeconds = InstantUtil.bucketSeconds(bucket);

        Instant bucketFrom = InstantUtil.truncateToBucket(from, bucket);
        Instant bucketTo   = InstantUtil.truncateToBucket(to.minusNanos(1), bucket)
                .plusSeconds(bucketSeconds);

        List<String> canonicalSensorTypes = sensorTypes;
        if (sensorTypes != null && !sensorTypes.isEmpty()) {
            canonicalSensorTypes = canonicalizeSensorTypes(compositeId, sensorTypes);
        }

        List<SensorAggregateResult> results = new ArrayList<>();
        if (canonicalSensorTypes == null || canonicalSensorTypes.isEmpty()) {
            results.addAll(aggregationReader.aggregate(compositeId, bucketFrom, bucketTo, bucket, null));
        } else {
            for (String st : canonicalSensorTypes) {
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

    private List<String> canonicalizeSensorTypes(String compositeId, List<String> sensorTypes) {
        List<LatestSensorValue> latestValues = latestSensorValueRepository.findByDevice_CompositeId(compositeId);
        if (latestValues.isEmpty()) {
            return new ArrayList<>(new LinkedHashSet<>(sensorTypes));
        }

        Map<String, String> canonicalByLower = new LinkedHashMap<>();
        for (LatestSensorValue value : latestValues) {
            String sensorType = value.getSensorType();
            if (sensorType == null) continue;
            canonicalByLower.putIfAbsent(sensorType.toLowerCase(Locale.ROOT), sensorType);
        }

        List<String> resolved = new ArrayList<>(sensorTypes.size());
        Set<String> dedup = new LinkedHashSet<>();
        for (String requested : sensorTypes) {
            if (requested == null) continue;
            String canonical = canonicalByLower.getOrDefault(requested.toLowerCase(Locale.ROOT), requested);
            if (dedup.add(canonical)) {
                resolved.add(canonical);
            }
        }
        return resolved;
    }

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
