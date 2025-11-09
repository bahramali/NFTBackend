package se.hydroleaf.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.hydroleaf.model.WaterFlowStatus;
import se.hydroleaf.repository.WaterFlowStatusRepository;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
public class WaterFlowStatusService {

    private final WaterFlowStatusRepository repository;

    public WaterFlowStatusService(WaterFlowStatusRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void recordStatus(String status, Instant timestamp, String sensorName, String sensorType) {
        String normalizedStatus = status != null ? status.trim() : null;
        if (normalizedStatus == null || normalizedStatus.isEmpty()) {
            log.warn("Skipping water_flow message without status (sensorName={})", sensorName);
            return;
        }

        Optional<WaterFlowStatus> latest = repository.findFirstByOrderByTimestampDescIdDesc();
        if (latest.isPresent() && sameStatus(latest.get(), normalizedStatus)) {
            log.debug("Skipping water_flow status '{}' because it matches the most recent entry", normalizedStatus);
            return;
        }

        WaterFlowStatus entity = WaterFlowStatus.builder()
                .value(normalizedStatus)
                .timestamp(timestamp != null ? timestamp : Instant.now())
                .sensorName(sensorName)
                .sensorType(sensorType)
                .build();

        repository.save(entity);
    }

    private boolean sameStatus(WaterFlowStatus lastStatus, String nextStatus) {
        String previous = lastStatus.getValue();
        if (previous == null) {
            return false;
        }
        return previous.equalsIgnoreCase(nextStatus);
    }
}

