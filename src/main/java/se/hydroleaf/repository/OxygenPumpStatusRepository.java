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
            SELECT status
            FROM oxygen_pump_status
            WHERE system= UPPER(:system) AND layer= UPPER(:layer)
            ORDER BY status_time DESC
            LIMIT 1;
            """, nativeQuery = true)
    AverageResult getLatestAverage(
            @Param("system") String system,
            @Param("layer") String layer
    );

    Optional<OxygenPumpStatus> findTopByOrderByIdAsc();
}
