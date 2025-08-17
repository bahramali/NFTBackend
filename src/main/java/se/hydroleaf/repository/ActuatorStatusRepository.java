package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.hydroleaf.model.ActuatorStatus;
import se.hydroleaf.repository.dto.LiveNowRow;

import java.util.List;
import java.util.Optional;

public interface ActuatorStatusRepository extends JpaRepository<ActuatorStatus, Long>, ActuatorStatusRepositoryCustom {

    /**
     * Latest actuator state for a device and actuator type.
     */
    Optional<ActuatorStatus> findTopByDeviceCompositeIdAndActuatorTypeOrderByTimestampDesc(String compositeId, String actuatorType);

    /**
     * Latest actuator averages across all systems and layers for the given types.
     */
    @Query(value = """
            WITH latest AS (
              SELECT
                a.composite_id,
                a.actuator_type,
                a.status_time,
                CASE WHEN a.state THEN 1.0 ELSE 0.0 END AS val,
                ROW_NUMBER() OVER (
                  PARTITION BY a.composite_id, a.actuator_type
                  ORDER BY a.status_time DESC
                ) AS rn
              FROM actuator_status a
              WHERE a.actuator_type IN (:types)
            )
            SELECT
              d.system AS system,
              d.layer AS layer,
              l.actuator_type AS sensor_type,
              MAX(l.status_time) AS last_update,
              AVG(l.val) AS avg_value
            FROM latest l
            JOIN device d ON d.composite_id = l.composite_id
            WHERE l.rn = 1
            GROUP BY d.system, d.layer, l.actuator_type
            """, nativeQuery = true)
    List<LiveNowRow> fetchLatestActuatorAverages(@Param("types") List<String> types);

}
