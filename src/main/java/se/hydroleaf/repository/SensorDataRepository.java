package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.hydroleaf.model.SensorData;
import se.hydroleaf.model.SensorRecord;

public interface SensorDataRepository extends JpaRepository<SensorData, Long> {
}
