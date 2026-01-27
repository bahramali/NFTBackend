package se.hydroleaf.repository;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.hydroleaf.model.DeviceStatusHistory;

@Repository
public interface DeviceStatusHistoryRepository extends JpaRepository<DeviceStatusHistory, Long> {

    List<DeviceStatusHistory> findByDevice_CompositeIdAndTimestampBetweenOrderByTimestampDesc(
            String compositeId,
            Instant from,
            Instant to);
}
