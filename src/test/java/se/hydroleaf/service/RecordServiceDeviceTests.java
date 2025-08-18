package se.hydroleaf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import se.hydroleaf.model.Device;
import se.hydroleaf.model.DeviceGroup;
import se.hydroleaf.repository.ActuatorStatusRepository;
import se.hydroleaf.repository.DeviceGroupRepository;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.SensorDataRepository;
import se.hydroleaf.repository.SensorHealthItemRepository;
import se.hydroleaf.model.DeviceType;
import se.hydroleaf.repository.dto.LiveNowRow;

import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for RecordService with Option-1 model:
 * - Device PK = composite_id
 * - RecordService.saveRecord persists SensorRecord + SensorData (+ optional actuator status)
 * Notes:
 * - All comments are in English only per your preference.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecordServiceDeviceTests {

    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    RecordService recordService;

    @Autowired
    DeviceRepository deviceRepository;
    @Autowired
    DeviceGroupRepository deviceGroupRepository;
    @Autowired
    SensorDataRepository sensorDataRepository;
    @Autowired
    ActuatorStatusRepository actuatorStatusRepository;
    @Autowired
    SensorHealthItemRepository sensorHealthItemRepository;
    private DeviceGroup defaultGroup;


    @BeforeEach
    void initGroup() {
        // create or reuse a group for tests
        defaultGroup = deviceGroupRepository.findByMqttTopic("test-group").orElseGet(() -> {
            DeviceGroup g = new DeviceGroup();
            g.setMqttTopic("test-group");
            return deviceGroupRepository.save(g);
        });
    }

    private Device ensureDevice(String compositeId) {
        return deviceRepository.findById(compositeId).orElseGet(() -> {
            Device d = new Device();
            d.setCompositeId(compositeId);
            String[] parts = compositeId.split("-");
            if (parts.length >= 2) {
                d.setSystem(parts[0]);
                d.setLayer(parts[1]);
                d.setDeviceId(parts[2]);
            }
            d.setGroup(defaultGroup);
            return deviceRepository.save(d);
        });
    }

    @Test
    void saveRecord_persists_values_and_pump_for_existing_device() throws Exception {
        // Arrange: make sure a Device exists with PK = composite_id
        final String compositeId = "S02-L02-G01";

        ensureDevice(compositeId);

        String json = """
                  {
                "timestamp": "2025-01-01T00:00:00Z",
                "sensors": [
                  {"sensorName": "lightSensor", "sensorType": "light", "value": 549.3, "unit": "lx"},
                  {"sensorName": "tempSensor",  "sensorType": "temperature", "value": 26.2, "unit": "°C"},
                  {"sensorName": "humSensor",   "sensorType": "humidity", "value": 42.4, "unit": "%"},
                  {"sensorType": "ph", "value": 6.5}
                ],
                "health": {
                  "tempSensor": true
                },
                "controllers": [
                  {"name": "airPump", "state": false}
                ]
                  }
                """;
        JsonNode node = objectMapper.readTree(json);

        long pumpBefore = actuatorStatusRepository.count();

        // Act
        recordService.saveRecord(compositeId, node);

        // Assert: device is still present and unchanged
        Device saved = deviceRepository.findById(compositeId).orElseThrow();
        assertEquals("S02", saved.getSystem());
        assertEquals("L02", saved.getLayer());
        assertEquals(compositeId, saved.getCompositeId());

        // Latest-average for a specific metric should include our record
        List<LiveNowRow> rows = sensorDataRepository.fetchLatestSensorAverages(List.of(DeviceType.LIGHT.getName()));
        LiveNowRow lightAvg = rows.stream()
                .filter(r -> "S02".equals(r.getSystem()) && "L02".equals(r.getLayer()))
                .findFirst().orElse(null);
        assertNotNull(lightAvg);
        assertNotNull(lightAvg.getAvgValue());
        assertTrue(lightAvg.getDeviceCount() >= 1);

        // sensorType persistence check (ph sensor without sensorName)
        assertTrue(sensorDataRepository.findAll().stream()
                .anyMatch(d -> "ph".equals(d.getSensorType())));

        // Pump status row should be inserted (airPump=false)
        long pumpAfter = actuatorStatusRepository.count();
        assertTrue(pumpAfter > pumpBefore, "pump status should be saved");
        var pumpRow = actuatorStatusRepository
                .findTopByDeviceCompositeIdAndActuatorTypeOrderByTimestampDesc(compositeId, "airPump")
                .orElseThrow();
        assertFalse(pumpRow.getState());
        assertEquals(Instant.parse("2025-01-01T00:00:00Z"), pumpRow.getTimestamp());
    }

    @Test
    void saveRecord_maps_sensor_health_by_sensorName() throws Exception {
        final String compositeId = "S03-L01-H01";
        ensureDevice(compositeId);

        String json = """
                {
                  "sensors":[
                    {"sensorName":"tempSensor","sensorType":"temperature","value":24.0},
                    {"sensorName":"phSensor","sensorType":"ph","value":6.8}
                  ],
                  "health":{
                    "tempSensor":false,
                    "phSensor":true
                  }
                }
                """;

        recordService.saveRecord(compositeId, objectMapper.readTree(json));

        var items = sensorHealthItemRepository.findAll();
        assertEquals(2, items.size());
        var tempItem = items.stream().filter(h -> "temperature".equals(h.getSensorType())).findFirst().orElseThrow();
        assertFalse(tempItem.getStatus());
        var phItem = items.stream().filter(h -> "ph".equals(h.getSensorType())).findFirst().orElseThrow();
        assertTrue(phItem.getStatus());
    }

    @Test
    void saveRecord_persists_pump_status_from_controllers_array() throws Exception {
        final String compositeId = "S04-L01-P01";
        ensureDevice(compositeId);

        String json = """
                {
                  "sensors":[{"sensorName":"s1","sensorType":"temperature","value":20.0}],
                  "health":{"s1":true},
                  "controllers":[{"name":"airPump","state":true,"timestamp":"2025-03-03T03:03:03Z"}]
                }
                """;

        long before = actuatorStatusRepository.count();
        recordService.saveRecord(compositeId, objectMapper.readTree(json));
        assertEquals(before + 1, actuatorStatusRepository.count());
        var pumpRow = actuatorStatusRepository
                .findTopByDeviceCompositeIdAndActuatorTypeOrderByTimestampDesc(compositeId, "airPump")
                .orElseThrow();
        assertTrue(pumpRow.getState());
        assertEquals(Instant.parse("2025-03-03T03:03:03Z"), pumpRow.getTimestamp());
    }

    @Test
    void saveRecord_skips_duplicate_sensor_types() throws Exception {
        final String compositeId = "S06-L01-DUP";
        ensureDevice(compositeId);

        String json = """
                {
                  "sensors": [
                    {"sensorName":"s1","sensorType":"temperature","value":20.0},
                    {"sensorName":"s2","sensorType":"temperature","value":30.0}
                  ]
                }
                """;

        recordService.saveRecord(compositeId, objectMapper.readTree(json));

        var data = sensorDataRepository.findAll().stream()
                .filter(d -> compositeId.equals(d.getRecord().getDevice().getCompositeId()))
                .toList();
        assertEquals(1, data.size());
        assertEquals("temperature", data.get(0).getSensorType());
        assertEquals(20.0, data.get(0).getValue());
    }

    @Test
    void saveRecord_skips_empty_or_null_sensor_types() throws Exception {
        final String compositeId = "S07-L01-EMP";
        ensureDevice(compositeId);

        String json = """
                {
                  "sensors": [
                    {"sensorName":"s1","sensorType":"","value":10.0},
                    {"sensorName":"s2","sensorType":null,"value":5.0},
                    {"sensorName":"s3","value":3.0},
                    {"sensorName":"s4","sensorType":"ph","value":6.0}
                  ]
                }
                """;

        recordService.saveRecord(compositeId, objectMapper.readTree(json));

        var data = sensorDataRepository.findAll().stream()
                .filter(d -> compositeId.equals(d.getRecord().getDevice().getCompositeId()))
                .toList();
        assertEquals(1, data.size());
        assertEquals("ph", data.get(0).getSensorType());
    }
}
