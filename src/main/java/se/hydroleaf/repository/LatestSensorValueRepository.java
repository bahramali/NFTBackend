package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.hydroleaf.model.LatestSensorValue;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository for LatestSensorValue. Primarily used in tests to populate the
 * latest_sensor_value table when database triggers are not available.
 */
public interface LatestSensorValueRepository extends JpaRepository<LatestSensorValue, Long> {

    Optional<LatestSensorValue> findByDevice_CompositeIdAndSensorType(String compositeId, String sensorType);

    List<LatestSensorValue> findByDevice_CompositeId(String compositeId);

    List<LatestSensorValue> findByDevice_CompositeIdIn(Collection<String> compositeIds);
}

