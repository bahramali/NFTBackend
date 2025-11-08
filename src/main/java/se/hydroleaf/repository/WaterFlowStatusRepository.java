package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.hydroleaf.model.WaterFlowStatus;

@Repository
public interface WaterFlowStatusRepository extends JpaRepository<WaterFlowStatus, Long> {
}

