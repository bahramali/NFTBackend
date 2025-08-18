package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.hydroleaf.model.ActuatorStatus;
import se.hydroleaf.repository.dto.LiveNowRow;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ActuatorStatusRepository extends JpaRepository<ActuatorStatus, Long>, ActuatorStatusRepositoryCustom {

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
                d.system,
                d.layer,
                a.actuator_type,
                CASE WHEN a.state THEN 1.0 ELSE 0.0 END AS numeric_state,
                'status' AS unit,
                a.status_time,
                ROW_NUMBER() OVER (
                  PARTITION BY a.composite_id, a.actuator_type
                  ORDER BY a.status_time DESC
                ) AS rn
              FROM actuator_status a
              JOIN device d ON d.composite_id = a.composite_id
              WHERE a.actuator_type IN (:types)
            )
            SELECT
              system AS system,
              layer AS layer,
              actuator_type AS sensor_type,
              MAX(unit) AS unit,
              AVG(numeric_state)::double precision AS avg_value,
              COUNT(*)::bigint AS device_count,
              MAX(status_time) AS record_time
            FROM latest
            WHERE rn = 1
            GROUP BY system, layer, actuator_type
            """, nativeQuery = true)
    List<LiveNowRow> fetchLatestActuatorAverages(@Param("types") Collection<String> types);

}
