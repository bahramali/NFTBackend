package se.hydroleaf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
                String sensorId = sensorNode.path("sensorId").asText();
                String type = sensorNode.path("type").asText();
                String unit = sensorNode.path("unit").asText();
                JsonNode valueNode = sensorNode.path("value");

                // If the value contains spectral information, split it into
                // blue light, red light, clear and NIR sensor data items
                if (valueNode.isObject()
                        && valueNode.has("415nm")
                        && valueNode.has("445nm")
                        && valueNode.has("480nm")
                        && valueNode.has("515nm")
                        && valueNode.has("555nm")
                        && valueNode.has("590nm")
                        && valueNode.has("630nm")
                        && valueNode.has("680nm")
                        && valueNode.has("clear")
                        && valueNode.has("nir")) {
                    double blue = (valueNode.path("415nm").asDouble()
                            + valueNode.path("445nm").asDouble()
                            + valueNode.path("480nm").asDouble()
                            + valueNode.path("515nm").asDouble()) / 4.0;
                    double red = (valueNode.path("555nm").asDouble()
                            + valueNode.path("590nm").asDouble()
                            + valueNode.path("630nm").asDouble()
                            + valueNode.path("680nm").asDouble()) / 4.0;
                    double clear = valueNode.path("clear").asDouble();
                    double nir = valueNode.path("nir").asDouble();

                    sensors.add(createSensorData(sensorId, "blueLight", unit, blue, record));
                    sensors.add(createSensorData(sensorId, "redLight", unit, red, record));
                    sensors.add(createSensorData(sensorId, "clear", unit, clear, record));
                    sensors.add(createSensorData(sensorId, "nir", unit, nir, record));
                } else {
                    SensorData sd = new SensorData();
                    sd.setSensorId(sensorId);
                    sd.setType(type);
                    sd.setUnit(unit);
                    sd.setValue(valueNode.toString());
                    sd.setRecord(record);
                    sensors.add(sd);
                }
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

    private SensorData createSensorData(String sensorId, String type, String unit,
                                        double value, SensorRecord record) {
        SensorData sd = new SensorData();
        sd.setSensorId(sensorId);
        sd.setType(type);
        sd.setUnit(unit);
        sd.setValue(Double.toString(value));
        sd.setRecord(record);
        return sd;
    }
}

