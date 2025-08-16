package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import se.hydroleaf.model.LatestActuatorStatus;

import java.util.Optional;

@Repository
public interface LatestActuatorStatusRepository extends JpaRepository<LatestActuatorStatus, Long> {

    Optional<LatestActuatorStatus> findByDeviceCompositeIdAndActuatorType(String compositeId, String actuatorType);

    @Query(value = """
            SELECT l.state FROM latest_actuator_status l
            JOIN device d ON d.composite_id = l.composite_id
            WHERE d.system = :system
              AND d.layer = :layer
              AND l.actuator_type = :actuatorType
            LIMIT 1
            """, nativeQuery = true)
    Boolean getLatestActuatorState(@Param("system") String system,
                                   @Param("layer") String layer,
                                   @Param("actuatorType") String actuatorType);
}
