package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.hydroleaf.model.SensorValueHistory;
import se.hydroleaf.model.SensorValueHistoryId;

public interface SensorValueHistoryRepository extends JpaRepository<SensorValueHistory, SensorValueHistoryId> {
}
