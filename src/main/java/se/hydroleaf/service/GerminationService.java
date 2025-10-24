package se.hydroleaf.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.hydroleaf.model.Device;
import se.hydroleaf.model.GerminationCycle;
import se.hydroleaf.model.TopicName;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.GerminationCycleRepository;
import se.hydroleaf.repository.dto.GerminationStatusResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Service
public class GerminationService {

    private final GerminationCycleRepository germinationCycleRepository;
    private final DeviceRepository deviceRepository;
    private final Clock clock;

    public GerminationService(GerminationCycleRepository germinationCycleRepository,
                              DeviceRepository deviceRepository,
                              Clock clock) {
        this.germinationCycleRepository = germinationCycleRepository;
        this.deviceRepository = deviceRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<GerminationStatusResponse> getStatus(String compositeId) {
        return germinationCycleRepository.findById(compositeId)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<GerminationStatusResponse> getStatus() {
        return getStatus(getGerminationDevice().getCompositeId());
    }

    @Transactional
    public GerminationStatusResponse triggerStart(String compositeId) {
        return updateStart(compositeId, Instant.now(clock));
    }

    @Transactional
    public GerminationStatusResponse triggerStart() {
        return triggerStart(getGerminationDevice().getCompositeId());
    }

    @Transactional
    public GerminationStatusResponse updateStart(String compositeId, Instant startTime) {
        Objects.requireNonNull(startTime, "startTime is required");

        Device device = deviceRepository.findById(compositeId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown device composite_id: " + compositeId));

        GerminationCycle cycle = germinationCycleRepository.findById(compositeId)
                .orElseGet(() -> new GerminationCycle(device, startTime));

        cycle.setStartTime(startTime);
        cycle.setDevice(device);
        cycle.setCompositeId(device.getCompositeId());

        germinationCycleRepository.save(cycle);

        return toResponse(cycle);
    }

    @Transactional
    public GerminationStatusResponse updateStart(Instant startTime) {
        return updateStart(getGerminationDevice().getCompositeId(), startTime);
    }

    private GerminationStatusResponse toResponse(GerminationCycle cycle) {
        Instant now = Instant.now(clock);
        long elapsedSeconds = Math.max(0, Duration.between(cycle.getStartTime(), now).getSeconds());
        return new GerminationStatusResponse(cycle.getCompositeId(), cycle.getStartTime(), elapsedSeconds);
    }

    private Device getGerminationDevice() {
        return deviceRepository.findFirstByTopic(TopicName.germinationTopic)
                .orElseThrow(() -> new IllegalStateException("No germination device configured"));
    }
}

