package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.hydroleaf.model.DeviceGroup;
import se.hydroleaf.model.SensorRecord;

import java.util.Optional;

public interface DeviceGroupRepository extends JpaRepository<DeviceGroup, Long> {
    Optional<DeviceGroup> findByMqttTopic(String topic);
}
