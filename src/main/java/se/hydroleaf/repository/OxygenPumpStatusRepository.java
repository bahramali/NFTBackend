package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.hydroleaf.model.OxygenPumpStatus;

@Repository
public interface OxygenPumpStatusRepository extends JpaRepository<OxygenPumpStatus, Long> {
}
