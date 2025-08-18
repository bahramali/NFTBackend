package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.hydroleaf.model.LatestSensorValue;

/**
 * Repository for LatestSensorValue. Primarily used in tests to populate the
 * latest_sensor_value table when database triggers are not available.
 */
public interface LatestSensorValueRepository extends JpaRepository<LatestSensorValue, Long> {
}

