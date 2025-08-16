package se.hydroleaf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import se.hydroleaf.model.Device;
import se.hydroleaf.model.DeviceGroup;
import se.hydroleaf.model.LatestActuatorStatus;
import se.hydroleaf.model.LatestSensorValue;
import se.hydroleaf.repository.DeviceGroupRepository;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.LatestActuatorStatusRepository;
import se.hydroleaf.repository.LatestSensorValueRepository;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/** Integration tests verifying that RecordService upserts latest tables. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecordServiceLatestTests {

    @Autowired ObjectMapper objectMapper;
    @Autowired RecordService recordService;
    @Autowired DeviceRepository deviceRepository;
    @Autowired DeviceGroupRepository deviceGroupRepository;
    @Autowired LatestSensorValueRepository latestSensorValueRepository;
    @Autowired LatestActuatorStatusRepository latestActuatorStatusRepository;

    private DeviceGroup ensureGroup() {
        return deviceGroupRepository.findByMqttTopic("test-group").orElseGet(() -> {
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
            d.setGroup(ensureGroup());
            return deviceRepository.save(d);
        });
    }

    @Test
    void upserts_latest_sensor_and_actuator() throws Exception {
        String compositeId = "S05-L01-T01";
        ensureDevice(compositeId);

        String first = """
                {
                  "timestamp":"2024-01-01T00:00:00Z",
                  "sensors":[{"sensorName":"lightSensor","sensorType":"light","value":10.0,"unit":"lx"}],
                  "controllers":[{"name":"airPump","state":true}]
                }
                """;
        recordService.saveRecord(compositeId, objectMapper.readTree(first));

        LatestSensorValue lsv = latestSensorValueRepository
                .findByDeviceCompositeIdAndSensorType(compositeId, "light")
                .orElseThrow();
        assertEquals(10.0, lsv.getValue());
        assertEquals("lx", lsv.getUnit());
        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), lsv.getTimestamp());

        LatestActuatorStatus las = latestActuatorStatusRepository
                .findByDeviceCompositeIdAndActuatorType(compositeId, "airPump")
                .orElseThrow();
        assertTrue(las.getState());
        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), las.getTimestamp());

        String second = """
                {
                  "timestamp":"2024-01-01T00:05:00Z",
                  "sensors":[{"sensorName":"lightSensor","sensorType":"light","value":15.5,"unit":"lx"}],
                  "controllers":[{"name":"airPump","state":false}]
                }
                """;
        recordService.saveRecord(compositeId, objectMapper.readTree(second));

        LatestSensorValue lsv2 = latestSensorValueRepository
                .findByDeviceCompositeIdAndSensorType(compositeId, "light")
                .orElseThrow();
        assertEquals(15.5, lsv2.getValue());
        assertEquals(Instant.parse("2024-01-01T00:05:00Z"), lsv2.getTimestamp());

        LatestActuatorStatus las2 = latestActuatorStatusRepository
                .findByDeviceCompositeIdAndActuatorType(compositeId, "airPump")
                .orElseThrow();
        assertFalse(las2.getState());
        assertEquals(Instant.parse("2024-01-01T00:05:00Z"), las2.getTimestamp());
    }
}

