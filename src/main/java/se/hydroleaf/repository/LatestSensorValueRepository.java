package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import se.hydroleaf.model.LatestSensorValue;

import java.util.Optional;

@Repository
public interface LatestSensorValueRepository extends JpaRepository<LatestSensorValue, Long> {

    Optional<LatestSensorValue> findByDeviceCompositeIdAndSensorType(String compositeId, String sensorType);

    @Query(value = """
            SELECT COALESCE(AVG(l.value),0) AS average,
                   COUNT(*)                AS count
            FROM latest_sensor_value l
            JOIN device d ON d.composite_id = l.composite_id
            WHERE d.system = :system
              AND d.layer = :layer
              AND l.sensor_type = :sensorType
            """, nativeQuery = true)
    AverageResult getLatestSensorAverage(@Param("system") String system,
                                         @Param("layer") String layer,
                                         @Param("sensorType") String sensorType);
}
