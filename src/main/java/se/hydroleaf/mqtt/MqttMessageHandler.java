package se.hydroleaf.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import se.hydroleaf.service.DeviceProvisionService;
import se.hydroleaf.service.RecordService;

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

    private final boolean publishEnabled;
    private final ObjectMapper objectMapper;
    private final RecordService recordService;
    private final SimpMessagingTemplate messagingTemplate;
    private final DeviceProvisionService deviceProvisionService;

    private final ConcurrentHashMap<String, Instant> lastSeen;

    public MqttMessageHandler(@Value("${mqtt.publishEnabled:true}") boolean publishEnabled,
                              ObjectMapper objectMapper,
                              RecordService recordService,
                              SimpMessagingTemplate messagingTemplate,
                              DeviceProvisionService deviceProvisionService,
                              ConcurrentHashMap<String, Instant> lastSeen) {
        this.publishEnabled = publishEnabled;
        this.objectMapper = objectMapper;
        this.recordService = recordService;
        this.messagingTemplate = messagingTemplate;
        this.deviceProvisionService = deviceProvisionService;
        this.lastSeen = lastSeen;
    }

    public void handle(String topic, String payload) {
        if (publishEnabled) {
            messagingTemplate.convertAndSend("/topic/" + topic, payload);
        } else {
            log.info("publishing to /topic/{}, payload: {}", topic, payload);
        }
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

    private static final Pattern WATER_PATTERN =
            Pattern.compile("^waterTank/(S\\d+)/(L\\d+)/([^/]+)$");

    private String deriveCompositeIdFromTopic(String topic) {
        if (topic == null) return null;
        Matcher m = GROW_PATTERN.matcher(topic);
        if (m.find()) {
            return m.group(1) + "-" + m.group(2) + "-" + m.group(3);
        }
        m = WATER_PATTERN.matcher(topic);
        if (m.find()) {
            return m.group(1) + "-" + m.group(2) + "-" + m.group(3);
        }
        return null;
    }

}

