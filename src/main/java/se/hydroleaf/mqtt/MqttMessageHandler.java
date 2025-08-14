package se.hydroleaf.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import se.hydroleaf.model.Device;
import se.hydroleaf.model.DeviceGroup;
import se.hydroleaf.repository.DeviceGroupRepository;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.service.RecordService;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles incoming MQTT messages: forwards payload to STOMP,
 * extracts compositeId, auto-provisions devices/groups and persists data.
 */
@Slf4j
@Component
public class MqttMessageHandler {

    private final ObjectMapper objectMapper;
    private final RecordService recordService;
    private final SimpMessagingTemplate messagingTemplate;
    private final DeviceRepository deviceRepo;
    private final DeviceGroupRepository groupRepo;

    @Value("${mqtt.topicPrefix:}")
    private String topicPrefix;

    private final ConcurrentHashMap<String, Instant> lastSeen = new ConcurrentHashMap<>();

    public MqttMessageHandler(ObjectMapper objectMapper,
                              RecordService recordService,
                              SimpMessagingTemplate messagingTemplate,
                              DeviceRepository deviceRepo,
                              DeviceGroupRepository groupRepo) {
        this.objectMapper = objectMapper;
        this.recordService = recordService;
        this.messagingTemplate = messagingTemplate;
        this.deviceRepo = deviceRepo;
        this.groupRepo = groupRepo;
    }

    public void handle(String topic, String payload) {
        messagingTemplate.convertAndSend("/topic/" + topic, payload);
        try {
            JsonNode node = objectMapper.readTree(payload);

            String compositeId = readCompositeId(node);
            if (compositeId == null) {
                compositeId = deriveCompositeIdFromTopic(topic);
            }
            if (compositeId == null || compositeId.isBlank()) {
                log.warn("MQTT parse/handle failed on topic {}: missing composite_id", topic);
                return;
            }

            ensureDevice(compositeId, topic);
            recordService.saveRecord(compositeId, node);
            lastSeen.put(compositeId, Instant.now());
        } catch (Exception ex) {
            log.error("MQTT handle error for topic {}: {}", topic, ex.getMessage(), ex);
        }
    }

    private static String readCompositeId(JsonNode n) {
        if (n == null) return null;
        if (n.hasNonNull("composite_id")) return n.get("composite_id").asText();
        if (n.hasNonNull("compositeId")) return n.get("compositeId").asText();
        return null;
    }

    private static final Pattern GROW_PATTERN =
            Pattern.compile("^growSensors/(S\\d+)/(L\\d+)/([^/]+)$");

    private String deriveCompositeIdFromTopic(String topic) {
        if (topic == null) return null;
        Matcher m = GROW_PATTERN.matcher(topic);
        if (m.find()) {
            return m.group(1) + "-" + m.group(2) + "-" + m.group(3);
        }
        return null;
    }

    private Device ensureDevice(String compositeId, String topic) {
        return deviceRepo.findById(compositeId).orElseGet(() -> {
            Device d = new Device();
            d.setCompositeId(compositeId);

            String[] parts = compositeId.split("-");
            if (parts.length >= 3) {
                d.setSystem(parts[0]);
                d.setLayer(parts[1]);
                d.setDeviceId(parts[2]);
            }

            String grpKey = (topic != null && !topic.isBlank()) ? topic.split("/")[0] : topicPrefix;
            DeviceGroup g = findOrCreateGroup(grpKey == null ? "default" : grpKey);
            d.setGroup(g);

            return deviceRepo.save(d);
        });
    }

    private DeviceGroup findOrCreateGroup(String mqttKey) {
        Optional<DeviceGroup> g = groupRepo.findByMqttTopic(mqttKey);
        if (g.isPresent()) return g.get();

        DeviceGroup ng = new DeviceGroup();
        ng.setMqttTopic(mqttKey);
        return groupRepo.save(ng);
    }

    @Scheduled(fixedDelay = 10000)
    public void logLaggingDevices() {
        Instant now = Instant.now();
        lastSeen.forEach((id, ts) -> {
            if (Duration.between(ts, now).toSeconds() > 60) {
                log.debug("Device {} no message for >60s (lastSeen={})", id, ts);
            }
        });
    }
}

