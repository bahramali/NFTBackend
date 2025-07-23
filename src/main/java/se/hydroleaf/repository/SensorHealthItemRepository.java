package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.hydroleaf.model.SensorHealthItem;
import se.hydroleaf.model.SensorRecord;

public interface SensorHealthItemRepository extends JpaRepository<SensorHealthItem, Long> {
}
