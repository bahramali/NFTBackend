package se.hydroleaf.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import se.hydroleaf.model.*;
import se.hydroleaf.repository.dto.LiveNowRow;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class LatestAveragesRepositoryTest {

    @MockBean
    private AggregateRepository aggregateRepository;

    @Autowired
    private SensorDataRepository sensorDataRepository;

    @Autowired
    private ActuatorStatusRepository actuatorStatusRepository;

    @Autowired
    private SensorRecordRepository sensorRecordRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private DeviceGroupRepository deviceGroupRepository;

    @Test
    void fetchLatestSensorAveragesReturnsTypedValues() {
        DeviceGroup group = new DeviceGroup();
        group.setMqttTopic("sensor-avg-group");
        group = deviceGroupRepository.save(group);

        Device device1 = new Device();
        device1.setCompositeId("SYS-L1-D1");
        device1.setSystem("SYS");
        device1.setLayer("L1");
        device1.setDeviceId("D1");
        device1.setGroup(group);
        deviceRepository.save(device1);

        Device device2 = new Device();
        device2.setCompositeId("SYS-L1-D2");
        device2.setSystem("SYS");
        device2.setLayer("L1");
        device2.setDeviceId("D2");
        device2.setGroup(group);
        deviceRepository.save(device2);

        Instant now = Instant.now();

        SensorRecord r1 = new SensorRecord();
        r1.setDevice(device1);
        r1.setTimestamp(now.minusSeconds(5));
        sensorRecordRepository.saveAndFlush(r1);

        SensorRecord r2 = new SensorRecord();
        r2.setDevice(device2);
        r2.setTimestamp(now.minusSeconds(3));
        sensorRecordRepository.saveAndFlush(r2);

        SensorData d1 = new SensorData();
        d1.setRecord(r1);
        d1.setSensorType("temperature");
        d1.setValue(10.0);
        d1.setUnit("C");
        sensorDataRepository.save(d1);

        SensorData d2 = new SensorData();
        d2.setRecord(r2);
        d2.setSensorType("temperature");
        d2.setValue(20.0);
        d2.setUnit("C");
        sensorDataRepository.save(d2);

        sensorDataRepository.flush();

        List<LiveNowRow> rows = sensorDataRepository.fetchLatestSensorAverages(List.of("temperature"));
        assertEquals(1, rows.size());
        LiveNowRow row = rows.get(0);

        assertInstanceOf(Double.class, row.avgValue());
        assertInstanceOf(Long.class, row.deviceCount());
        assertTrue(row.recordTime() instanceof Timestamp || row.recordTime() instanceof OffsetDateTime,
                "recordTime should be Timestamp or OffsetDateTime");

        assertEquals(15.0, row.getAvgValue());
        assertEquals(2L, row.getDeviceCount());
        assertNotNull(row.getRecordTime());
        assertInstanceOf(Instant.class, row.getRecordTime());
    }

    @Test
    void fetchLatestActuatorAveragesReturnsTypedValues() {
        DeviceGroup group = new DeviceGroup();
        group.setMqttTopic("actuator-avg-group");
        group = deviceGroupRepository.save(group);

        Device device1 = new Device();
        device1.setCompositeId("SYS-L1-A1");
        device1.setSystem("SYS");
        device1.setLayer("L1");
        device1.setDeviceId("A1");
        device1.setGroup(group);
        deviceRepository.save(device1);

        Device device2 = new Device();
        device2.setCompositeId("SYS-L1-A2");
        device2.setSystem("SYS");
        device2.setLayer("L1");
        device2.setDeviceId("A2");
        device2.setGroup(group);
        deviceRepository.save(device2);

        Instant now = Instant.now();

        ActuatorStatus s1 = new ActuatorStatus();
        s1.setDevice(device1);
        s1.setActuatorType("pump");
        s1.setState(true);
        s1.setTimestamp(now.minusSeconds(4));
        actuatorStatusRepository.save(s1);

        ActuatorStatus s2 = new ActuatorStatus();
        s2.setDevice(device2);
        s2.setActuatorType("pump");
        s2.setState(false);
        s2.setTimestamp(now.minusSeconds(2));
        actuatorStatusRepository.save(s2);

        actuatorStatusRepository.flush();

        List<LiveNowRow> rows = actuatorStatusRepository.fetchLatestActuatorAverages(List.of("pump"));
        assertEquals(1, rows.size());
        LiveNowRow row = rows.get(0);

        assertInstanceOf(Double.class, row.avgValue());
        assertInstanceOf(Long.class, row.deviceCount());
        assertTrue(row.recordTime() instanceof Timestamp || row.recordTime() instanceof OffsetDateTime,
                "recordTime should be Timestamp or OffsetDateTime");

        assertEquals(0.5, row.getAvgValue());
        assertEquals(2L, row.getDeviceCount());
        assertNotNull(row.getRecordTime());
        assertInstanceOf(Instant.class, row.getRecordTime());
    }
}

