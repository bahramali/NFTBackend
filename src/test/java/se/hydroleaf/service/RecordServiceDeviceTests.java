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
import se.hydroleaf.repository.DeviceGroupRepository;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.OxygenPumpStatusRepository;
import se.hydroleaf.repository.SensorDataRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for RecordService with Option-1 model:
 * - Device PK = composite_id
 * - RecordService.saveRecord persists SensorRecord + SensorData (+ optional pump)
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
    OxygenPumpStatusRepository oxygenPumpStatusRepository;
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
                "values": {
                  "light":       {"value": 549.3, "unit": "lx"},
                  "temperature": {"value": 26.2,  "unit": "°C"},
                  "humidity":    {"value": 42.4,  "unit": "%"}
                },
                    "health": {
                  "temperature": true
                },
                "air_pump": false
                  }
                """;
        JsonNode node = objectMapper.readTree(json);

        long pumpBefore = oxygenPumpStatusRepository.count();

        // Act
        recordService.saveRecord(compositeId, node);

        // Assert: device is still present and unchanged
        Device saved = deviceRepository.findById(compositeId).orElseThrow();
        assertEquals("S02", saved.getSystem());
        assertEquals("L02", saved.getLayer());
        assertEquals(compositeId, saved.getCompositeId());

        // Latest-average for a specific metric should include our record
        var lightAvg = sensorDataRepository.getLatestAverage("S02", "L02", "light");
        assertNotNull(lightAvg);
        assertNotNull(lightAvg.getAverage());
        assertTrue(lightAvg.getCount() >= 1);

        // Pump status row should be inserted (air_pump=false)
        long pumpAfter = oxygenPumpStatusRepository.count();
        assertTrue(pumpAfter > pumpBefore, "pump status should be saved");
    }
}
