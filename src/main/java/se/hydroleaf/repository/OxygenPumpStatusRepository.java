package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import se.hydroleaf.model.OxygenPumpStatus;

import java.util.Optional;

@Repository
public interface OxygenPumpStatusRepository extends JpaRepository<OxygenPumpStatus, Long> {

    /**
     * Latest pump status for a single device (by composite_id).
     */
    Optional<OxygenPumpStatus> findTopByDeviceCompositeIdOrderByTimestampDesc(String compositeId);

    /**
     * Latest pump status across devices of a system/layer as ON ratio in [0..1].
     * Postgres DISTINCT ON picks the latest row per device.
     * If you dropped device.system/layer, replace WHERE with: ops.composite_id LIKE :prefix
     * (e.g. 'S01-L02-%').
     */
    @Query(value = """
            WITH latest AS (
              SELECT
                ops.composite_id,
                ops.status,
                ops.status_time,
                ROW_NUMBER() OVER (
                  PARTITION BY ops.composite_id
                  ORDER BY ops.status_time DESC
                ) AS rn
              FROM oxygen_pump_status ops
              JOIN device d ON d.composite_id = ops.composite_id
              WHERE d.system = :system AND d.layer = :layer
            )
            SELECT
              COALESCE(AVG(CASE WHEN status THEN 1 ELSE 0 END), 0)  AS average,
              CAST(COUNT(*) AS INTEGER)                              AS count
            FROM latest
            WHERE rn = 1                                             -- latest per device
            """, nativeQuery = true)
    AverageResult getLatestPumpAverage(
            @Param("system") String system,
            @Param("layer") String layer
    );

}
