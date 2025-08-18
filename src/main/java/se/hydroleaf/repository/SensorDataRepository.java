package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.hydroleaf.model.SensorData;
import se.hydroleaf.repository.dto.LiveNowRow;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SensorDataRepository extends JpaRepository<SensorData, Long>, SensorDataRepositoryCustom {

    /**
     * Latest sensor reading for a device and sensor type.
     */
    Optional<SensorData> findTopByRecord_DeviceCompositeIdAndSensorTypeOrderByRecord_TimestampDesc(String compositeId,
                                                                                                   String sensorType);

    /**
     * Batch query returning the latest average per system/layer and sensor type.
     */
    @Query(value = """
            WITH latest AS (
              SELECT
                d.system,
                d.layer,
                sd.sensor_type,
                sd.sensor_value AS value,
                ROW_NUMBER() OVER (
                  PARTITION BY sr.device_composite_id, sd.sensor_type
                  ORDER BY sr.record_time DESC
                ) AS rn
              FROM sensor_data sd
              JOIN sensor_record sr ON sr.id = sd.record_id
              JOIN device d ON d.composite_id = sr.device_composite_id
              WHERE sd.sensor_type IN (:types)
            )
            SELECT
              system AS system,
              layer AS layer,
              sensor_type AS type,
              AVG(value) AS average,
              COUNT(*)::bigint AS count
            FROM latest
            WHERE rn = 1
            GROUP BY system, layer, sensor_type
            """, nativeQuery = true)
    List<LiveNowRow> fetchLatestSensorAverages(@Param("types") Collection<String> types);
}
