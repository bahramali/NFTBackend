package se.hydroleaf.repository;

import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.hydroleaf.model.DeviceEvent;

@Repository
public interface DeviceEventRepository extends JpaRepository<DeviceEvent, Long> {

    Page<DeviceEvent> findByDevice_CompositeIdAndEventTimeBetween(
            String compositeId,
            Instant from,
            Instant to,
            Pageable pageable);
}
