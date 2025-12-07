package se.hydroleaf.shelly.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import se.hydroleaf.shelly.dto.ShellyScheduleRequest;
import se.hydroleaf.shelly.dto.ShellyScheduleResponse;
import se.hydroleaf.shelly.exception.ShellyException;
import se.hydroleaf.shelly.model.ShellyDeviceConfig;
import se.hydroleaf.shelly.registry.ShellyDeviceRegistry;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class ShellyScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ShellyScheduleService.class);

    private final ShellyClientService clientService;
    private final ShellyDeviceRegistry deviceRegistry;
    private final TaskScheduler taskScheduler;
    private final Map<String, List<ScheduledFuture<?>>> scheduledTasks = new ConcurrentHashMap<>();

    public ShellyScheduleService(ShellyClientService clientService, ShellyDeviceRegistry deviceRegistry, TaskScheduler taskScheduler) {
        this.clientService = clientService;
        this.deviceRegistry = deviceRegistry;
        this.taskScheduler = taskScheduler;
    }

    public ShellyScheduleResponse scheduleDevice(String deviceId, ShellyScheduleRequest request) {
        ShellyDeviceConfig device = resolveDevice(deviceId);

        LocalDateTime turnOnAt = Optional.ofNullable(request.getTurnOnAt()).orElse(LocalDateTime.now());
        LocalDateTime turnOffAt = request.getTurnOffAt();

        if (turnOffAt == null) {
            Long durationMinutes = request.getDurationMinutes();
            if (durationMinutes == null || durationMinutes <= 0) {
                throw new IllegalArgumentException("Either turnOffAt or a positive durationMinutes must be provided");
            }
            turnOffAt = turnOnAt.plusMinutes(durationMinutes);
        }

        if (turnOffAt.isBefore(turnOnAt)) {
            throw new IllegalArgumentException("turnOffAt must be after turnOnAt");
        }

        scheduleTask(device, turnOnAt, true);
        scheduleTask(device, turnOffAt, false);

        return ShellyScheduleResponse.builder()
                .deviceId(deviceId)
                .scheduled(true)
                .turnOnAt(turnOnAt)
                .turnOffAt(turnOffAt)
                .build();
    }

    private ShellyDeviceConfig resolveDevice(String deviceId) {
        return deviceRegistry.getDevice(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown device id: " + deviceId));
    }

    private void scheduleTask(ShellyDeviceConfig device, LocalDateTime time, boolean turnOn) {
        ScheduledFuture<?> future = taskScheduler.schedule(() -> executeCommand(device, turnOn), Date.from(time.atZone(ZoneId.systemDefault()).toInstant()));
        scheduledTasks.computeIfAbsent(device.getId(), key -> Collections.synchronizedList(new ArrayList<>())).add(future);
    }

    private void executeCommand(ShellyDeviceConfig device, boolean turnOn) {
        try {
            if (turnOn) {
                clientService.turnOn(device);
                log.info("Scheduled ON executed for {}", device.getId());
            } else {
                clientService.turnOff(device);
                log.info("Scheduled OFF executed for {}", device.getId());
            }
        } catch (ShellyException ex) {
            log.error("Shelly command failed for {} ({}): {}", device.getId(), device.getIp(), ex.getMessage());
        }
    }
}
