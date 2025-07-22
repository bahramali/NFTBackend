package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.hydroleaf.model.SensorRecord;

public interface SensorRecordRepository extends JpaRepository<SensorRecord, Long> {
}
