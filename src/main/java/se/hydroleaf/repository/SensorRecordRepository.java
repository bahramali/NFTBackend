package se.hydroleaf.repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import se.hydroleaf.model.SensorRecord;

public interface SensorRecordRepository extends JpaRepository<SensorRecord, Long> {
    Optional<SensorRecord> findTopByOrderByTimestampDesc();

    List<SensorRecord> findByTimestampBetween(Instant from, Instant to);
}
