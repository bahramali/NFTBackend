package se.hydroleaf.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.hydroleaf.model.WaterFlowStatus;
import se.hydroleaf.repository.WaterFlowStatusRepository;

import java.time.Instant;

@Slf4j
@Service
public class WaterFlowStatusService {

    private final WaterFlowStatusRepository repository;

    public WaterFlowStatusService(WaterFlowStatusRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void recordStatus(String status, Instant timestamp, String source) {
        if (status == null || status.isBlank()) {
            log.warn("Skipping water_flow message without status (source={})", source);
            return;
        }

        WaterFlowStatus entity = WaterFlowStatus.builder()
                .status(status)
                .timestamp(timestamp != null ? timestamp : Instant.now())
                .source(source)
                .build();

        repository.save(entity);
    }
}

