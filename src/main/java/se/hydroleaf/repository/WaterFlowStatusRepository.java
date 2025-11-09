package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import se.hydroleaf.model.WaterFlowStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaterFlowStatusRepository extends JpaRepository<WaterFlowStatus, Long> {

    Optional<WaterFlowStatus> findFirstByOrderByTimestampDescIdDesc();

    @Query("SELECT DISTINCT w.sensorType FROM WaterFlowStatus w WHERE w.sensorType IS NOT NULL")
    List<String> findDistinctSensorTypes();
}
