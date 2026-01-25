package se.hydroleaf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import se.hydroleaf.model.Device;
import se.hydroleaf.model.LatestSensorValue;
import se.hydroleaf.model.TopicName;
import se.hydroleaf.repository.ActuatorStatusRepository;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.SensorValueHistoryRepository;
import se.hydroleaf.repository.LatestSensorValueRepository;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecordServiceDeviceTests {

    @Autowired ObjectMapper objectMapper;
    @Autowired RecordService recordService;
    @Autowired DeviceRepository deviceRepository;
    @Autowired SensorValueHistoryRepository sensorValueHistoryRepository;
    @Autowired ActuatorStatusRepository actuatorStatusRepository;
    @Autowired LatestSensorValueRepository latestSensorValueRepository;
    @Autowired SensorValueBuffer sensorValueBuffer;

    @BeforeEach
    void initData() {
        sensorValueBuffer.flush();
        sensorValueHistoryRepository.deleteAll();
    }

    @AfterEach
    void clearBuffer() {
        sensorValueBuffer.flush();
        sensorValueHistoryRepository.deleteAll();
    }

    private Device ensureDevice(String compositeId) {
        return deviceRepository.findById(compositeId).orElseGet(() -> {
            Device d = new Device();
            d.setCompositeId(compositeId);
            String[] parts = compositeId.split("-");
            if (parts.length >= 4) {
                d.setSystem(parts[0]);
                d.setRack(parts[1]);
                d.setLayer(parts[2]);
                d.setDeviceId(parts[3]);
            }
            d.setTopic(TopicName.growSensors);
            return deviceRepository.save(d);
        });
    }

    @Test
    void saveRecord_persists_values_and_pump_for_existing_device() throws Exception {
        final String compositeId = "S02-R01-L02-G01";
        ensureDevice(compositeId);

        String json = """
                  {
                    "timestamp": "2025-01-01T00:00:00Z",
                    "lux": 549.3,
                    "layer_temp_c": 26.2,
                    "rh_pct": 42.4,
                    "co2_ppm": 812,
                    "as7343_counts": {
                      "405nm": 101.0,
                      "VIS1": 202.0
                    },
                    "controllers": [
                      {"name": "airPump", "state": false}
                    ]
                  }
                """;
        JsonNode node = objectMapper.readTree(json);

        long pumpBefore = actuatorStatusRepository.count();
        recordService.saveRecord(compositeId, node, TopicName.growSensors);

        // buffer should delay persistence until flush
        assertEquals(0, sensorValueHistoryRepository.count());

        Device saved = deviceRepository.findById(compositeId).orElseThrow();
        assertEquals("S02", saved.getSystem());
        assertEquals("R01", saved.getRack());
        assertEquals("L02", saved.getLayer());
        assertEquals(compositeId, saved.getCompositeId());

        LatestSensorValue luxValue = latestSensorValueRepository
                .findByDevice_CompositeIdAndSensorType(compositeId, "lux")
                .orElseThrow();
        assertEquals(549.3, luxValue.getValue());
        assertEquals("lux", luxValue.getUnit());

        sensorValueBuffer.flush();
        assertTrue(sensorValueHistoryRepository.findAll().stream()
                .anyMatch(d -> "as7343_counts_405nm".equals(d.getSensorType())));

        long pumpAfter = actuatorStatusRepository.count();
        assertTrue(pumpAfter > pumpBefore);
        var pumpRow = actuatorStatusRepository
                .findTopByDeviceCompositeIdAndActuatorTypeOrderByTimestampDesc(compositeId, "airPump")
                .orElseThrow();
        assertFalse(pumpRow.getState());
        assertEquals(Instant.parse("2025-01-01T00:00:00Z"), pumpRow.getTimestamp());
    }

    @Test
    void buffered_readings_are_averaged_on_flush() throws Exception {
        final String compositeId = "S10-R01-L10-AVG";
        ensureDevice(compositeId);

        String first = """
                {"timestamp":"2025-01-01T00:00:00Z","lux":6.0}
                """;
        String second = """
                {"timestamp":"2025-01-01T00:00:30Z","lux":8.0}
                """;
        recordService.saveRecord(compositeId, objectMapper.readTree(first), TopicName.growSensors);
        recordService.saveRecord(compositeId, objectMapper.readTree(second), TopicName.growSensors);

        // nothing persisted until flush
        assertEquals(0, sensorValueHistoryRepository.count());

        sensorValueBuffer.flush();

        var all = sensorValueHistoryRepository.findAll();
        assertEquals(1, all.size());
        assertEquals("lux", all.get(0).getSensorType());
        assertEquals(7.0, all.get(0).getSensorValue());
        assertEquals(Instant.parse("2025-01-01T00:00:00Z"), all.get(0).getValueTime());
    }

    @Test
    void saveRecord_upserts_latest_sensor_value() throws Exception {
        final String compositeId = "S08-R01-L01-LSV";
        ensureDevice(compositeId);

        String first = """
                {
                  "timestamp":"2025-05-05T05:05:05Z",
                  "air_temp_c": 21.5
                }
                """;
        recordService.saveRecord(compositeId, objectMapper.readTree(first), TopicName.growSensors);

        LatestSensorValue v1 = latestSensorValueRepository
                .findByDevice_CompositeIdAndSensorType(compositeId, "air_temp_c")
                .orElseThrow();
        assertEquals(21.5, v1.getValue());
        assertEquals("C", v1.getUnit());
        assertEquals(Instant.parse("2025-05-05T05:05:05Z"), v1.getValueTime());

        String second = """
                {
                  "timestamp":"2025-05-06T06:06:06Z",
                  "air_temp_c": 22.0
                }
                """;
        recordService.saveRecord(compositeId, objectMapper.readTree(second), TopicName.growSensors);

        LatestSensorValue v2 = latestSensorValueRepository
                .findByDevice_CompositeIdAndSensorType(compositeId, "air_temp_c")
                .orElseThrow();
        assertEquals(22.0, v2.getValue());
        assertEquals(Instant.parse("2025-05-06T06:06:06Z"), v2.getValueTime());
    }

    @Test
    void unknown_device_is_auto_registered() throws Exception {
        final String compositeId = "S20-R01-L20-NEW";
        assertTrue(deviceRepository.findById(compositeId).isEmpty());

        JsonNode json = objectMapper.readTree("{}\n");
        recordService.saveRecord(compositeId, json, TopicName.waterTank);

        Device saved = deviceRepository.findById(compositeId).orElse(null);
        assertNotNull(saved);
        assertEquals("S20", saved.getSystem());
        assertEquals("R01", saved.getRack());
        assertEquals("L20", saved.getLayer());
        assertEquals("NEW", saved.getDeviceId());
        assertEquals(TopicName.waterTank, saved.getTopic());
    }
}
