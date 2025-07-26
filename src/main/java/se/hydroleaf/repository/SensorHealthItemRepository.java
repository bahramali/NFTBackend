package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.hydroleaf.model.SensorHealthItem;

public interface SensorHealthItemRepository extends JpaRepository<SensorHealthItem, Long> {
}
