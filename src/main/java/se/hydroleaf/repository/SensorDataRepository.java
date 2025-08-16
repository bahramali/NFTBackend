package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.hydroleaf.model.SensorData;

import java.util.Optional;

public interface SensorDataRepository extends JpaRepository<SensorData, Long>, SensorDataRepositoryCustom {

    /**
     * Latest sensor reading for a device and sensor type.
     */
    Optional<SensorData> findTopByRecord_DeviceCompositeIdAndSensorTypeOrderByRecord_TimestampDesc(String compositeId,
                                                                                                   String sensorType);
}
