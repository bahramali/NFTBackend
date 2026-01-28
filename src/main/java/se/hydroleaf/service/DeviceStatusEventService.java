package se.hydroleaf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.hydroleaf.model.Device;
import se.hydroleaf.model.DeviceEvent;
import se.hydroleaf.model.DeviceStatusHistory;
import se.hydroleaf.model.TopicName;
import se.hydroleaf.repository.DeviceEventRepository;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.DeviceStatusHistoryRepository;
import se.hydroleaf.repository.dto.report.DeviceEventResponse;
import se.hydroleaf.repository.dto.report.DeviceStatusHistoryResponse;

@Service
public class DeviceStatusEventService {

    private static final Logger log = LoggerFactory.getLogger(DeviceStatusEventService.class);

    private final DeviceRepository deviceRepository;
    private final DeviceStatusHistoryRepository statusHistoryRepository;
    private final DeviceEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public DeviceStatusEventService(DeviceRepository deviceRepository,
                                    DeviceStatusHistoryRepository statusHistoryRepository,
                                    DeviceEventRepository eventRepository,
                                    ObjectMapper objectMapper) {
        this.deviceRepository = deviceRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void recordStatus(String compositeId, String status, Instant statusTime) {
        Objects.requireNonNull(compositeId, "compositeId is required");
        String normalizedId = normalizeCompositeId(compositeId);
        Device device = resolveDevice(normalizedId);
        String normalizedStatus = normalizeStatus(status);
        Instant timestamp = statusTime != null ? statusTime : Instant.now();

        DeviceStatusHistory history = new DeviceStatusHistory();
        history.setDevice(device);
        history.setStatus(normalizedStatus);
        history.setTimestamp(timestamp);
        statusHistoryRepository.save(history);
    }

    @Transactional
    public void recordEvent(String compositeId,
                            Instant eventTime,
                            String level,
                            String code,
                            String msg,
                            String raw) {
        Objects.requireNonNull(compositeId, "compositeId is required");
        String normalizedId = normalizeCompositeId(compositeId);
        Device device = resolveDevice(normalizedId);
        Instant timestamp = eventTime != null ? eventTime : Instant.now();

        DeviceEvent event = new DeviceEvent();
        event.setDevice(device);
        event.setEventTime(timestamp);
        event.setLevel(level);
        event.setCode(code);
        event.setMsg(msg);
        event.setRaw(raw);
        eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<DeviceStatusHistoryResponse> getStatusHistory(String compositeId, Instant from, Instant to) {
        String normalizedId = normalizeCompositeId(compositeId);
        requireDevice(normalizedId);

        return statusHistoryRepository
                .findByDevice_CompositeIdAndTimestampBetweenOrderByTimestampDesc(normalizedId, from, to)
                .stream()
                .map(history -> new DeviceStatusHistoryResponse(
                        history.getDevice().getCompositeId(),
                        history.getStatus(),
                        history.getTimestamp()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DeviceEventResponse> getEvents(String compositeId, Instant from, Instant to, int limit) {
        String normalizedId = normalizeCompositeId(compositeId);
        requireDevice(normalizedId);

        int safeLimit = Math.max(1, limit);
        PageRequest page = PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "eventTime"));

        return eventRepository
                .findByDevice_CompositeIdAndEventTimeBetween(normalizedId, from, to, page)
                .stream()
                .map(event -> new DeviceEventResponse(
                        event.getDevice().getCompositeId(),
                        event.getEventTime(),
                        event.getLevel(),
                        event.getCode(),
                        event.getMsg(),
                        parseRaw(event.getRaw())))
                .collect(Collectors.toList());
    }

    private Device resolveDevice(String compositeId) {
        Device device = deviceRepository.findById(compositeId)
                .orElseGet(() -> autoRegisterDevice(compositeId));
        ensureTopicForRack(device, device.getRack());
        return device;
    }

    private void requireDevice(String compositeId) {
        if (!deviceRepository.existsById(compositeId)) {
            throw new IllegalArgumentException("Unknown device composite_id: " + compositeId);
        }
    }

    private Device autoRegisterDevice(String compositeId) {
        String[] parts = compositeId.split("-", 4);
        if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid compositeId: " + compositeId);
        }
        Device device = new Device();
        device.setCompositeId(compositeId);
        device.setSystem(parts[0]);
        device.setRack(parts[1]);
        device.setLayer(parts[2]);
        device.setDeviceId(parts[3]);
        device.setTopic(resolveTopicForRack(parts[1], TopicName.growSensors));
        deviceRepository.save(device);
        log.info("Auto-registered unknown device {}", compositeId);
        return device;
    }

    private void ensureTopicForRack(Device device, String rack) {
        if (device == null || rack == null) {
            return;
        }
        TopicName desired = resolveTopicForRack(rack, device.getTopic());
        if (desired != null && desired != device.getTopic()) {
            device.setTopic(desired);
            deviceRepository.save(device);
        }
    }

    private TopicName resolveTopicForRack(String rack, TopicName fallback) {
        if (rack != null && rack.equalsIgnoreCase("germination")) {
            return TopicName.germinationTopic;
        }
        return fallback;
    }

    private String normalizeCompositeId(String compositeId) {
        String[] parts = compositeId.split("-", 4);
        if (parts.length == 4) {
            return compositeId;
        }
        if (parts.length == 3) {
            return String.format("%s-UNKNOWN-%s-%s", parts[0], parts[1], parts[2]);
        }
        throw new IllegalArgumentException("Invalid compositeId: " + compositeId);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "unknown";
        }
        String trimmed = status.trim();
        if (trimmed.length() <= 32) {
            return trimmed.toLowerCase(Locale.ROOT);
        }
        return trimmed.substring(0, 32).toLowerCase(Locale.ROOT);
    }

    private JsonNode parseRaw(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ex) {
            log.warn("Unable to parse raw event payload", ex);
            return null;
        }
    }
}
