package se.hydroleaf.shelly.service;

import java.time.Duration;
import java.time.Instant;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import se.hydroleaf.shelly.dto.AutomationRequest;
import se.hydroleaf.shelly.dto.AutomationResponse;
import se.hydroleaf.shelly.model.AutomationDefinition;
import se.hydroleaf.shelly.model.AutomationType;
import se.hydroleaf.shelly.model.IntervalMode;
import se.hydroleaf.shelly.model.ScheduledAutomation;
import se.hydroleaf.shelly.model.SocketDevice;
import se.hydroleaf.shelly.registry.ShellyRegistry;
import se.hydroleaf.shelly.service.ShellyClient;

@Service
public class ShellyAutomationService {

    private static final Logger log = LoggerFactory.getLogger(ShellyAutomationService.class);
    private static final ZoneId STOCKHOLM_ZONE = ZoneId.of("Europe/Stockholm");

    private final ShellyRegistry registry;
    private final ShellyClient clientService;
    private final TaskScheduler taskScheduler;

    private final Map<String, ScheduledAutomation> automations = new ConcurrentHashMap<>();

    public ShellyAutomationService(ShellyRegistry registry, ShellyClient clientService, TaskScheduler taskScheduler) {
        this.registry = registry;
        this.clientService = clientService;
        this.taskScheduler = taskScheduler;
    }

    public AutomationResponse createAutomation(AutomationRequest request) {
        SocketDevice device = registry.getSocket(request.getSocketId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown socket id: " + request.getSocketId()));

        AutomationDefinition definition = buildDefinition(request);
        ScheduledAutomation scheduledAutomation = scheduleAutomation(device, definition);
        automations.put(definition.getAutomationId(), scheduledAutomation);

        return toResponse(definition);
    }

    public List<AutomationResponse> listAutomations() {
        return automations.values().stream()
                .map(ScheduledAutomation::getDefinition)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public void deleteAutomation(String automationId) {
        ScheduledAutomation scheduledAutomation = automations.remove(automationId);
        if (scheduledAutomation != null) {
            scheduledAutomation.cancelAll();
        }
    }

    private ScheduledAutomation scheduleAutomation(SocketDevice device, AutomationDefinition definition) {
        ScheduledAutomation scheduledAutomation = new ScheduledAutomation(definition);
        switch (definition.getType()) {
            case TIME_RANGE -> scheduleTimeRange(device, definition, scheduledAutomation);
            case INTERVAL_TOGGLE -> scheduleInterval(device, definition, scheduledAutomation);
            case AUTO_OFF -> scheduleAutoOff(device, definition, scheduledAutomation);
        }
        return scheduledAutomation;
    }

    private void scheduleTimeRange(
            SocketDevice device, AutomationDefinition definition, ScheduledAutomation scheduledAutomation) {
        Set<DayOfWeek> onDays = definition.getDaysOfWeek();
        boolean crossesMidnight = definition.getOffTime().isBefore(definition.getOnTime());
        Set<DayOfWeek> offDays = crossesMidnight ? shiftDays(onDays) : onDays;
        LocalTime onTime = definition.getOnTime();
        LocalTime offTime = definition.getOffTime();

        String onCron = cronForTime(onTime, onDays);
        String offCron = cronForTime(offTime, offDays);

        ScheduledFuture<?> onFuture = taskScheduler.schedule(
                () -> safeToggle(device, true, definition), new CronTrigger(onCron, STOCKHOLM_ZONE));
        ScheduledFuture<?> offFuture = taskScheduler.schedule(
                () -> safeToggle(device, false, definition), new CronTrigger(offCron, STOCKHOLM_ZONE));

        scheduledAutomation.addFuture(onFuture);
        scheduledAutomation.addFuture(offFuture);
    }

    private void scheduleInterval(
            SocketDevice device, AutomationDefinition definition, ScheduledAutomation scheduledAutomation) {
        Runnable task;
        if (definition.getMode() == IntervalMode.PULSE) {
            task = () -> {
                safeToggle(device, true, definition);
                int pulseSeconds = definition.getPulseSeconds() == null ? 5 : definition.getPulseSeconds();
                ScheduledFuture<?> pulseFuture = taskScheduler.schedule(
                        () -> safeToggle(device, false, definition),
                        Instant.now().plusSeconds(pulseSeconds));
                scheduledAutomation.addFuture(pulseFuture);
            };
        } else {
            task = () -> {
                try {
                    boolean nextState = !clientService.getStatus(device).isOutput();
                    safeToggle(device, nextState, definition);
                } catch (Exception ex) {
                    log.error("Interval automation {} failed for {}: {}",
                            definition.getAutomationId(), device.getId(), ex.getMessage());
                }
            };
        }

        ScheduledFuture<?> intervalFuture = taskScheduler.scheduleAtFixedRate(
                task, Instant.now(), Duration.ofMinutes(definition.getIntervalMinutes()));
        scheduledAutomation.addFuture(intervalFuture);
    }

    private void scheduleAutoOff(
            SocketDevice device, AutomationDefinition definition, ScheduledAutomation scheduledAutomation) {
        boolean startNow = definition.getStartNow() == null || definition.getStartNow();
        if (startNow) {
            safeToggle(device, true, definition);
        }
        ScheduledFuture<?> offFuture = taskScheduler.schedule(
                () -> safeToggle(device, false, definition),
                Instant.now().plus(Duration.ofMinutes(definition.getDurationMinutes())));
        scheduledAutomation.addFuture(offFuture);
    }

    private void safeToggle(SocketDevice device, boolean turnOn, AutomationDefinition definition) {
        try {
            if (turnOn) {
                clientService.turnOn(device);
            } else {
                clientService.turnOff(device);
            }
        } catch (Exception ex) {
            log.error("Automation {} failed for {}: {}", definition.getAutomationId(), device.getId(), ex.getMessage());
        }
    }

    private AutomationDefinition buildDefinition(AutomationRequest request) {
        AutomationType type = request.getType();
        String automationId = UUID.randomUUID().toString();
        switch (type) {
            case TIME_RANGE -> {
                LocalTime onTime = request.getOnTime();
                LocalTime offTime = request.getOffTime();
                if (onTime == null || offTime == null) {
                    throw new IllegalArgumentException("onTime and offTime are required for TIME_RANGE automations");
                }
                if (onTime.equals(offTime)) {
                    throw new IllegalArgumentException("onTime and offTime must differ");
                }
                return AutomationDefinition.builder()
                        .automationId(automationId)
                        .type(type)
                        .socketId(request.getSocketId())
                        .onTime(onTime)
                        .offTime(offTime)
                        .daysOfWeek(normalizeDays(request.getDaysOfWeek()))
                        .build();
            }
            case INTERVAL_TOGGLE -> {
                if (request.getIntervalMinutes() == null || request.getIntervalMinutes() <= 0) {
                    throw new IllegalArgumentException("intervalMinutes must be greater than 0 for INTERVAL_TOGGLE");
                }
                IntervalMode mode = request.getMode() == null ? IntervalMode.TOGGLE : request.getMode();
                return AutomationDefinition.builder()
                        .automationId(automationId)
                        .type(type)
                        .socketId(request.getSocketId())
                        .intervalMinutes(request.getIntervalMinutes())
                        .mode(mode)
                        .pulseSeconds(request.getPulseSeconds())
                        .build();
            }
            case AUTO_OFF -> {
                if (request.getDurationMinutes() == null || request.getDurationMinutes() <= 0) {
                    throw new IllegalArgumentException("durationMinutes must be greater than 0 for AUTO_OFF");
                }
                return AutomationDefinition.builder()
                        .automationId(automationId)
                        .type(type)
                        .socketId(request.getSocketId())
                        .durationMinutes(request.getDurationMinutes())
                        .startNow(request.getStartNow())
                        .build();
            }
            default -> throw new IllegalArgumentException("Unsupported automation type: " + type);
        }
    }

    private AutomationResponse toResponse(AutomationDefinition definition) {
        String description = switch (definition.getType()) {
            case TIME_RANGE ->
                    "Daily " + definition.getOnTime() + " -> " + definition.getOffTime();
            case INTERVAL_TOGGLE -> definition.getMode() == IntervalMode.PULSE
                    ? "Pulse every " + definition.getIntervalMinutes() + " min"
                    : "Toggle every " + definition.getIntervalMinutes() + " min";
            case AUTO_OFF -> "Auto-off after " + definition.getDurationMinutes() + " min";
        };

        Set<String> days = definition.getDaysOfWeek() == null
                ? null
                : definition.getDaysOfWeek().stream().map(this::cronToken).collect(Collectors.toSet());

        return AutomationResponse.builder()
                .automationId(definition.getAutomationId())
                .type(definition.getType())
                .socketId(definition.getSocketId())
                .description(description)
                .daysOfWeek(days)
                .intervalMinutes(definition.getIntervalMinutes())
                .mode(definition.getMode())
                .pulseSeconds(definition.getPulseSeconds())
                .durationMinutes(definition.getDurationMinutes())
                .createdAt(Instant.now())
                .build();
    }

    private String cronForTime(LocalTime time, Set<DayOfWeek> days) {
        String joinedDays = days.stream().map(this::cronToken).collect(Collectors.joining(","));
        return String.format("0 %d %d ? * %s", time.getMinute(), time.getHour(), joinedDays);
    }

    private Set<DayOfWeek> normalizeDays(Set<String> days) {
        if (days == null || days.isEmpty()) {
            return Set.of(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY,
                    DayOfWeek.SATURDAY,
                    DayOfWeek.SUNDAY);
        }

        return days.stream().map(this::parseDay).collect(Collectors.toSet());
    }

    private Set<DayOfWeek> shiftDays(Set<DayOfWeek> days) {
        return days.stream().map(day -> day.plus(1)).collect(Collectors.toSet());
    }

    private DayOfWeek parseDay(String value) {
        String upper = value.toUpperCase();
        return switch (upper) {
            case "MON" -> DayOfWeek.MONDAY;
            case "TUE", "TUES" -> DayOfWeek.TUESDAY;
            case "WED" -> DayOfWeek.WEDNESDAY;
            case "THU", "THUR", "THURS" -> DayOfWeek.THURSDAY;
            case "FRI" -> DayOfWeek.FRIDAY;
            case "SAT" -> DayOfWeek.SATURDAY;
            case "SUN" -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("Invalid day of week: " + value);
        };
    }

    private String cronToken(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "MON";
            case TUESDAY -> "TUE";
            case WEDNESDAY -> "WED";
            case THURSDAY -> "THU";
            case FRIDAY -> "FRI";
            case SATURDAY -> "SAT";
            case SUNDAY -> "SUN";
        };
    }
}
