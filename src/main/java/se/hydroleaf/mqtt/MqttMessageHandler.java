package se.hydroleaf.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import se.hydroleaf.service.DeviceProvisionService;
import se.hydroleaf.service.RecordService;

import java.time.Duration;
import java.time.Instant;
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
    private final DeviceProvisionService deviceProvisionService;

    private final ConcurrentHashMap<String, Instant> lastSeen = new ConcurrentHashMap<>();

    public MqttMessageHandler(ObjectMapper objectMapper,
                              RecordService recordService,
                              SimpMessagingTemplate messagingTemplate,
                              DeviceProvisionService deviceProvisionService) {
        this.objectMapper = objectMapper;
        this.recordService = recordService;
        this.messagingTemplate = messagingTemplate;
        this.deviceProvisionService = deviceProvisionService;
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

            deviceProvisionService.ensureDevice(compositeId, topic);
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

