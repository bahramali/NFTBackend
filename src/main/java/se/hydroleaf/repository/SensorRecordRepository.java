package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.hydroleaf.model.SensorRecord;

import java.time.Instant;
import java.util.List;

public interface SensorRecordRepository extends JpaRepository<SensorRecord, Long> {
    List<SensorRecord> findByDevice_CompositeIdAndTimestampBetween(String compositeId, Instant from, Instant to);
}
