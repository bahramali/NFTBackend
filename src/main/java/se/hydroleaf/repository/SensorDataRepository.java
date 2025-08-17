package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.hydroleaf.model.SensorData;
import se.hydroleaf.repository.dto.LiveNowRow;

import java.util.List;
import java.util.Optional;

public interface SensorDataRepository extends JpaRepository<SensorData, Long>, SensorDataRepositoryCustom {

    /**
     * Latest sensor reading for a device and sensor type.
     */
    Optional<SensorData> findTopByRecord_DeviceCompositeIdAndSensorTypeOrderByRecord_TimestampDesc(String compositeId,
                                                                                                   String sensorType);

    @Query(value = """
            WITH latest AS (
              SELECT
                d.system         AS system,
                d.layer          AS layer,
                sr.device_composite_id AS composite_id,
                sd.sensor_type   AS sensor_type,
                sd.unit          AS unit,
                sd.sensor_value  AS val,
                sr.record_time   AS record_time,
                ROW_NUMBER() OVER (
                  PARTITION BY d.system, d.layer, sr.device_composite_id, sd.sensor_type
                  ORDER BY sr.record_time DESC
                ) AS rn
              FROM sensor_record sr
              JOIN sensor_data sd ON sd.record_id = sr.id
              JOIN device d ON d.composite_id = sr.device_composite_id
              WHERE sd.sensor_type IN (:types)
                AND sd.sensor_value IS NOT NULL
            )
            SELECT
              system,
              layer,
              sensor_type,
              unit,
              AVG(val)                          AS avg_value,
              CAST(COUNT(val) AS BIGINT)        AS device_count,
              MAX(record_time)                  AS record_time
            FROM latest
            WHERE rn = 1
            GROUP BY system, layer, sensor_type, unit
            """, nativeQuery = true)
    List<LiveNowRow> fetchLatestSensorAverages(@Param("types") List<String> types);
}
