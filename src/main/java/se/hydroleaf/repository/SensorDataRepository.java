package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import se.hydroleaf.model.SensorData;

import java.util.Optional;

@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, Long> {

    /**
     * Latest sensor reading for a device and sensor type.
     */
    Optional<SensorData> findTopByRecord_DeviceCompositeIdAndSensorTypeOrderByRecord_TimestampDesc(String compositeId,
                                                                                                   String sensorType);

    @Query(value = """
            WITH last_rec AS (
              SELECT
                sr.id,
                sr.device_composite_id,
                ROW_NUMBER() OVER (
                  PARTITION BY sr.device_composite_id
                  ORDER BY sr.record_time DESC
                ) AS rn
              FROM sensor_record sr
              JOIN device d ON d.composite_id = sr.device_composite_id
              WHERE d.system = :system AND d.layer = :layer
            )
            SELECT
              COALESCE(AVG(sd.sensor_value), 0)                     AS average,
              CAST(COUNT(sd.sensor_value) AS INTEGER)               AS count
            FROM last_rec lr
            JOIN sensor_data sd ON sd.record_id = lr.id
            WHERE lr.rn = 1                                         -- latest per device
              AND sd.sensor_type = :sensorType
            """, nativeQuery = true)
    AverageResult getLatestAverage(
            @Param("system") String system,
            @Param("layer") String layer,
            @Param("sensorType") String sensorType
    );

}
