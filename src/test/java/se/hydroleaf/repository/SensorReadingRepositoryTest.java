package se.hydroleaf.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import se.hydroleaf.model.Device;
import se.hydroleaf.model.DeviceGroup;
import se.hydroleaf.model.SensorReading;
import se.hydroleaf.model.SensorRecord;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
class SensorReadingRepositoryTest {

    @Autowired
    private SensorReadingRepository sensorReadingRepository;

    @Autowired
    private SensorRecordRepository sensorRecordRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private DeviceGroupRepository deviceGroupRepository;

    @Test
    void duplicateRecordAndSensorTypeFails() {
        DeviceGroup group = new DeviceGroup();
        group.setMqttTopic("unique-constraint-test-group");
        group = deviceGroupRepository.save(group);

        Device device = new Device();
        device.setCompositeId("S01-L01-D01");
        device.setSystem("S01");
        device.setLayer("L01");
        device.setDeviceId("D01");
        device.setGroup(group);
        device = deviceRepository.save(device);

        SensorRecord record = new SensorRecord();
        record.setDevice(device);
        record.setTimestamp(Instant.now());
        record = sensorRecordRepository.save(record);

        SensorReading first = new SensorReading();
        first.setRecord(record);
        first.setSensorType("light");
        first.setValue(1.0);
        sensorReadingRepository.saveAndFlush(first);

        SensorReading second = new SensorReading();
        second.setRecord(record);
        second.setSensorType("light");
        second.setValue(2.0);

        assertThrows(DataIntegrityViolationException.class, () -> {
            sensorReadingRepository.saveAndFlush(second);
        });
    }
}
