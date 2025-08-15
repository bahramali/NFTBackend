package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import se.hydroleaf.model.ActuatorStatus;

import java.util.Optional;

@Repository
public interface ActuatorStatusRepository extends JpaRepository<ActuatorStatus, Long> {

    /**
     * Latest actuator state for a device and actuator type.
     */
    Optional<ActuatorStatus> findTopByDeviceCompositeIdAndActuatorTypeOrderByTimestampDesc(String compositeId, String actuatorType);

    /**
     * Latest actuator state across devices of a system/layer as ON ratio in [0..1].
     * Postgres DISTINCT ON or window functions to pick the latest row per device.
     */
    @Query(value = """
            WITH latest AS (
              SELECT
                a.composite_id,
                a.state,
                a.status_time,
                ROW_NUMBER() OVER (
                  PARTITION BY a.composite_id
                  ORDER BY a.status_time DESC
                ) AS rn
              FROM actuator_status a
              JOIN device d ON d.composite_id = a.composite_id
              WHERE d.system = :system AND d.layer = :layer AND a.actuator_type = :actuatorType
            )
            SELECT
              COALESCE(AVG(CASE WHEN state THEN 1 ELSE 0 END), 0) AS average,
              CAST(COUNT(*) AS INTEGER)                             AS count
            FROM latest
            WHERE rn = 1
            """, nativeQuery = true)
    AverageResult getLatestActuatorAverage(@Param("system") String system,
                                           @Param("layer") String layer,
                                           @Param("actuatorType") String actuatorType);
}
