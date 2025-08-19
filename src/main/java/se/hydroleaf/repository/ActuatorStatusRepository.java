package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.hydroleaf.model.ActuatorStatus;
import se.hydroleaf.repository.dto.LiveNowRow;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ActuatorStatusRepository extends JpaRepository<ActuatorStatus, Long> {

    /**
     * Latest actuator state for a device and actuator type.
     */
    Optional<ActuatorStatus> findTopByDeviceCompositeIdAndActuatorTypeOrderByTimestampDesc(String compositeId, String actuatorType);

    /**
     * Batch query returning the latest actuator averages per system/layer.
     */
    @Query(value = """
            WITH latest AS (
              SELECT
                composite_id,
                actuator_type,
                CASE WHEN state THEN 1.0 ELSE 0.0 END AS numeric_state,
                'status' AS unit,
                status_time
              FROM (
                SELECT
                  composite_id,
                  actuator_type,
                  state,
                  status_time,
                  ROW_NUMBER() OVER (
                    PARTITION BY composite_id, actuator_type
                    ORDER BY status_time DESC
                  ) AS rn
                FROM actuator_status
                WHERE actuator_type IN (:types)
              ) ranked
              WHERE rn = 1
            )
            SELECT
              d.system AS system,
              d.layer AS layer,
              l.actuator_type AS sensor_type,
              MAX(l.unit) AS unit,
              AVG(l.numeric_state)::double precision AS avg_value,
              COUNT(*)::bigint AS device_count,
              MAX(l.status_time) AS record_time
            FROM latest l
            JOIN device d ON d.composite_id = l.composite_id
            GROUP BY d.system, d.layer, l.actuator_type
            """, nativeQuery = true)
    List<LiveNowRow> fetchLatestActuatorAverages(@Param("types") Collection<String> types);

}
