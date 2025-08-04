package se.hydroleaf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.hydroleaf.dto.TimestampValue;
import se.hydroleaf.dto.AggregatedHistoryResponse;
import se.hydroleaf.dto.AggregatedSensorData;
import se.hydroleaf.model.*;
import se.hydroleaf.repository.*;
import se.hydroleaf.util.InstantUtil;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class RecordService {

    private final DeviceRepository deviceRepository;
    private final DeviceGroupRepository deviceGroupRepository;
    private final SensorRecordRepository recordRepository;
    private final SensorDataRepository sensorDataRepository;
    private final ObjectMapper objectMapper;

    private static final long TARGET_POINTS = 300;
    private static final long SAMPLE_INTERVAL_MS = 5000;

    public RecordService(DeviceRepository deviceRepository, DeviceGroupRepository deviceGroupRepository, SensorRecordRepository recordRepository, SensorDataRepository sensorDataRepository, ObjectMapper objectMapper) {
        this.deviceRepository = deviceRepository;
        this.deviceGroupRepository = deviceGroupRepository;
        this.recordRepository = recordRepository;
        this.sensorDataRepository = sensorDataRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void saveMessage(String topic, String json) {
        try {
            JsonNode node = objectMapper.readTree(json);

            // Find or create device group based on topic
            DeviceGroup group = deviceGroupRepository.findByMqttTopic(topic)
                    .orElseGet(() -> {
                        DeviceGroup g = new DeviceGroup();
                        g.setMqttTopic(topic);
                        return deviceGroupRepository.save(g);
                    });
log.info("DeviceGroup: {}", group);
            // Find or create device
            String deviceId = node.path("deviceId").asText();
            Device device = deviceRepository.findById(deviceId).orElseGet(() -> {
                Device d = new Device();
                d.setId(deviceId);
                d.setGroup(group);
                return deviceRepository.save(d);
            });
log.info("device: {}", device);

            device.setLocation(node.path("location").asText());
            device.setSystem(node.path("system").asText());

            // Create sensor record
            SensorRecord record = new SensorRecord();
            record.setTimestamp(InstantUtil.parse(node.path("timestamp").asText()));
            record.setDevice(device);

            // Parse sensors
            List<SensorData> sensors = new ArrayList<>();
            for (JsonNode sensorNode : node.path("sensors")) {
                String sensorName = sensorNode.path("sensorName").asText();
                String valueType = sensorNode.path("valueType").asText();
                String unit = sensorNode.path("unit").asText();
                JsonNode valueNode = sensorNode.path("value");
                JsonNode sourceNode = sensorNode.path("source");

                SensorData sd = new SensorData();
                sd.setSensorName(sensorName);
                sd.setValueType(valueType);
                sd.setUnit(unit);
                sd.setValue(valueNode.asDouble());
                if (!sourceNode.isMissingNode()) {
                    sd.setSource(sourceNode.asText());
                }
                sd.setRecord(record);
                sensors.add(sd);
            }
            record.setSensors(sensors);

// Parse health map
            JsonNode healthNode = node.path("health");
            if (!healthNode.isMissingNode() && healthNode.isObject()) {
                List<SensorHealthItem> healthItems = new ArrayList<>();
                healthNode.fields().forEachRemaining(entry -> {
                    SensorHealthItem item = new SensorHealthItem();
                    item.setSensorType(entry.getKey());
                    item.setStatus(entry.getValue().asBoolean());
                    item.setRecord(record);
                    healthItems.add(item);
                });
                record.setHealth(healthItems);
            }
            recordRepository.save(record);
            log.info("saved in database record: {}",record);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse and save message", e);
        }
    }

    @Transactional(readOnly = true)
    public AggregatedHistoryResponse getAggregatedRecords(String deviceId, Instant from, Instant to) {
        long durationMs = to.toEpochMilli() - from.toEpochMilli();
        long approxIntervalMs = Math.max(SAMPLE_INTERVAL_MS, durationMs / TARGET_POINTS);
        long bucketSizeSeconds = approxIntervalMs / 1000;
        log.info("start search in sensorDataRepository.aggregateSensorData(deviceId, from, to, bucketSizeSeconds={})", bucketSizeSeconds);
        List<SensorAggregateResult> results = sensorDataRepository.aggregateSensorData(deviceId, from, to, bucketSizeSeconds);

        Map<String, AggregatedSensorData> map = new LinkedHashMap<>();
        for (SensorAggregateResult r : results) {
            // Include unit in the aggregation key to avoid mixing data from sensors
            // that may share name and value type but report values in different units
            String key = r.getSensorName() + "|" + r.getValueType() + "|" + r.getUnit();
            AggregatedSensorData agg = map.computeIfAbsent(key, k ->
                    new AggregatedSensorData(r.getSensorName(), r.getValueType(), r.getUnit(), new ArrayList<>())
            );
            agg.data().add(new TimestampValue(r.getBucketTime(), r.getAvgValue()));
        }

        return new AggregatedHistoryResponse(from, to, new ArrayList<>(map.values()));
    }
}
