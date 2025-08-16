package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.hydroleaf.model.ActuatorStatus;

import java.util.Optional;

@Repository
public interface ActuatorStatusRepository extends JpaRepository<ActuatorStatus, Long>, ActuatorStatusRepositoryCustom {

    /**
     * Latest actuator state for a device and actuator type.
     */
    Optional<ActuatorStatus> findTopByDeviceCompositeIdAndActuatorTypeOrderByTimestampDesc(String compositeId, String actuatorType);

}
