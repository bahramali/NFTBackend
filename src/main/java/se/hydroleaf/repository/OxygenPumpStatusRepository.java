package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import se.hydroleaf.model.OxygenPumpStatus;

import java.util.Optional;

@Repository
public interface OxygenPumpStatusRepository extends JpaRepository<OxygenPumpStatus, Long> {

    @Query(value = """
            SELECT CASE WHEN status = TRUE THEN 1 ELSE 0 END AS average, 1 AS count
            FROM oxygen_pump_status
            WHERE system = UPPER(:system) AND layer = UPPER(:layer)
            LIMIT 1
            """, nativeQuery = true)
    AverageResult getLatestAverage(
            @Param("system") String system,
            @Param("layer") String layer
    );

    Optional<OxygenPumpStatus> findTopByOrderByIdAsc();
}
