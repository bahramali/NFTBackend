package se.hydroleaf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.hydroleaf.dto.TimestampValue;
import se.hydroleaf.dto.AggregatedHistoryResponse;
import se.hydroleaf.dto.AggregatedSensorData;
import se.hydroleaf.model.*;
import se.hydroleaf.repository.*;

import java.time.Instant;
import java.util.*;

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

            // Find or create device
            String deviceId = node.path("deviceId").asText();
            Device device = deviceRepository.findById(deviceId).orElseGet(() -> {
                Device d = new Device();
                d.setId(deviceId);
                d.setGroup(group);
                return deviceRepository.save(d);
            });
            device.setLocation(node.path("location").asText());

            // Create sensor record
            SensorRecord record = new SensorRecord();
            record.setTimestamp(Instant.parse(node.path("timestamp").asText()));
            record.setDevice(device);

            // Parse sensors
            List<SensorData> sensors = new ArrayList<>();
            for (JsonNode sensorNode : node.path("sensors")) {
                SensorData sd = new SensorData();
                sd.setSensorId(sensorNode.path("sensorId").asText());
                sd.setType(sensorNode.path("type").asText());
                sd.setUnit(sensorNode.path("unit").asText());
                sd.setValue(sensorNode.path("value").toString());
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
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse and save message", e);
        }
    }

    @Transactional(readOnly = true)
    public AggregatedHistoryResponse getAggregatedRecords(String deviceId, Instant from, Instant to) {
        long durationMs = to.toEpochMilli() - from.toEpochMilli();
        long approxIntervalMs = Math.max(SAMPLE_INTERVAL_MS, durationMs / TARGET_POINTS);

        List<SensorAggregateResult> results = sensorDataRepository.aggregateSensorData(deviceId, from, to, approxIntervalMs);

        Map<String, AggregatedSensorData> map = new LinkedHashMap<>();
        for (SensorAggregateResult r : results) {
            String key = r.getSensorId() + "|" + r.getType();
            AggregatedSensorData agg = map.computeIfAbsent(key, k ->
                    new AggregatedSensorData(r.getSensorId(), r.getType(), r.getUnit(), new ArrayList<>())
            );
            agg.data().add(new TimestampValue(r.getBucketTime(), r.getAvgValue()));
        }

        return new AggregatedHistoryResponse(from, to, new ArrayList<>(map.values()));
    }
}
