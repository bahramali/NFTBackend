package se.hydroleaf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.hydroleaf.dto.SensorDataResponse;
import se.hydroleaf.dto.SensorRecordResponse;
import se.hydroleaf.dto.TimestampValue;
import se.hydroleaf.dto.AggregatedHistoryResponse;
import se.hydroleaf.dto.AggregatedSensorData;
import se.hydroleaf.model.*;
import se.hydroleaf.repository.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@Service
public class RecordService {

    private final DeviceRepository deviceRepository;
    private final DeviceGroupRepository deviceGroupRepository;
    private final SensorRecordRepository recordRepository;
    private final ObjectMapper objectMapper;

    public RecordService(DeviceRepository deviceRepository,
                         DeviceGroupRepository deviceGroupRepository,
                         SensorRecordRepository recordRepository,
                         ObjectMapper objectMapper) {
        this.deviceRepository = deviceRepository;
        this.deviceGroupRepository = deviceGroupRepository;
        this.recordRepository = recordRepository;
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

    private SensorDataResponse mapSensorData(SensorData data) {
        Object value;
        try {
            value = objectMapper.readValue(data.getValue(), Object.class);
        } catch (Exception e) {
            value = data.getValue();
        }
        return new SensorDataResponse(
                data.getSensorId(),
                data.getType(),
                value,
                data.getUnit(),
                null
        );
    }

    private SensorRecordResponse mapRecord(SensorRecord record) {
        List<SensorDataResponse> sensorDtos = record.getSensors().stream()
                .map(this::mapSensorData)
                .toList();
        return new SensorRecordResponse(record.getTimestamp(), sensorDtos);
    }

    private Object parseValue(SensorData data) {
        try {
            return objectMapper.readValue(data.getValue(), Object.class);
        } catch (Exception e) {
            return data.getValue();
        }
    }

    @Transactional(readOnly = true)
    public List<SensorRecordResponse> getRecords(String deviceId, Instant from, Instant to) {
        return recordRepository.findByDevice_IdAndTimestampBetween(deviceId, from, to)
                .stream()
                .map(this::mapRecord)
                .toList();
    }

    @Transactional(readOnly = true)
    public AggregatedHistoryResponse getAggregatedRecords(String deviceId, Instant from, Instant to) {
        List<SensorRecord> records = recordRepository.findByDevice_IdAndTimestampBetween(deviceId, from, to);

        java.util.Map<String, AggregatedSensorData> map = new java.util.LinkedHashMap<>();
        for (SensorRecord record : records) {
            for (SensorData data : record.getSensors()) {
                String key = data.getSensorId() + "|" + data.getType();
                AggregatedSensorData agg = map.get(key);
                if (agg == null) {
                    agg = new AggregatedSensorData(
                            data.getSensorId(),
                            data.getType(),
                            data.getUnit(),
                            new java.util.ArrayList<>()
                    );
                    map.put(key, agg);
                }
                agg.data().add(new TimestampValue(record.getTimestamp(), parseValue(data)));
            }
        }

        return new AggregatedHistoryResponse(
                from,
                to,
                new java.util.ArrayList<>(map.values())
        );
    }
}
