package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import se.hydroleaf.model.OxygenPumpStatus;

@Repository
public interface OxygenPumpStatusRepository extends JpaRepository<OxygenPumpStatus, Long> {

    @Query(value = """
            SELECT AVG(val) AS average, COUNT(*) AS count
            FROM (
                SELECT (CASE WHEN status THEN 1 ELSE 0 END) AS val
                FROM oxygen_pump_status
                WHERE LOWER(system) = LOWER(:system) AND LOWER(layer) = LOWER(:layer)
                ORDER BY status_time DESC
                LIMIT 1
            ) latest
            """, nativeQuery = true)
    AverageResult getLatestAverage(
            @Param("system") String system,
            @Param("layer") String layer
    );
}
