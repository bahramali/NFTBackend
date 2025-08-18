package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.hydroleaf.model.SensorData;
import se.hydroleaf.repository.dto.LiveNowRow;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SensorDataRepository extends JpaRepository<SensorData, Long> {

    /**
     * Latest sensor reading for a device and sensor type.
     */
    Optional<SensorData> findTopByRecord_DeviceCompositeIdAndSensorTypeOrderByRecord_TimestampDesc(String compositeId,
                                                                                                   String sensorType);

    /**
     * Batch query returning the latest average per system/layer and sensor type.
     */
    @Query(value = """
            SELECT
              d.system AS system,
              d.layer AS layer,
              lsv.sensor_type AS sensor_type,
              MAX(lsv.unit) AS unit,
              AVG(lsv.sensor_value)::double precision AS avg_value,
              COUNT(*)::bigint AS device_count,
              MAX(lsv.value_time) AS record_time
            FROM latest_sensor_value lsv
            JOIN device d ON d.composite_id = lsv.composite_id
            WHERE lsv.sensor_type IN (:types)
            GROUP BY d.system, d.layer, lsv.sensor_type
            """, nativeQuery = true)
    List<LiveNowRow> fetchLatestSensorAverages(@Param("types") Collection<String> types);
}
