package se.hydroleaf.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import se.hydroleaf.dto.snapshot.LiveNowSnapshot;
import se.hydroleaf.model.*;
import se.hydroleaf.repository.*;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import(StatusService.class)
class StatusServiceIntegrationTest {

    @MockBean
    private AggregateRepository aggregateRepository;

    @Autowired
    private StatusService statusService;

    @Autowired
    private SensorDataRepository sensorDataRepository;

    @Autowired
    private ActuatorStatusRepository actuatorStatusRepository;

    @Autowired
    private DeviceGroupRepository deviceGroupRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private SensorRecordRepository sensorRecordRepository;

/*    @Test
    void getLiveNowSnapshotReturnsData() {
        DeviceGroup group = new DeviceGroup();
        group.setMqttTopic("group1");
        group = deviceGroupRepository.save(group);

        Device device = new Device();
        device.setCompositeId("S01-L01-D01");
        device.setSystem("S01");
        device.setLayer("L01");
        device.setDeviceId("D01");
        device.setGroup(group);
        device = deviceRepository.save(device);

        Instant now = Instant.now();

        SensorRecord record = new SensorRecord();
        record.setDevice(device);
        record.setTimestamp(now);
        record = sensorRecordRepository.save(record);

        SensorData data = new SensorData();
        data.setRecord(record);
        data.setSensorType("light");
        data.setValue(5.0);
        data.setUnit("lux");
        sensorDataRepository.save(data);

        ActuatorStatus status = new ActuatorStatus();
        status.setDevice(device);
        status.setActuatorType("airPump");
        status.setState(true);
        status.setTimestamp(now);
        actuatorStatusRepository.save(status);

        LiveNowSnapshot snapshot = statusService.getLiveNowSnapshot();

        assertNotNull(snapshot);
//        assertTrue(snapshot.systems().containsKey("S01"));
        assertEquals(5.0, snapshot.systems().get("S01").environment().light().average());
        assertEquals(1.0, snapshot.systems().get("S01").actuators().airPump().average());
    }*/
}
