package se.hydroleaf.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import se.hydroleaf.service.DeviceProvisionService;
import se.hydroleaf.service.RecordService;

import se.hydroleaf.scheduler.LastSeenRegistry;
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
    private final TopicPublisher topicPublisher;
    private final DeviceProvisionService deviceProvisionService;

    private final LastSeenRegistry lastSeen;

    public MqttMessageHandler(ObjectMapper objectMapper,
                              RecordService recordService,
                              TopicPublisher topicPublisher,
                              DeviceProvisionService deviceProvisionService,
                              LastSeenRegistry lastSeen) {
        this.objectMapper = objectMapper;
        this.recordService = recordService;
        this.topicPublisher = topicPublisher;
        this.deviceProvisionService = deviceProvisionService;
        this.lastSeen = lastSeen;
    }

    public void handle(String topic, String payload) {
        topicPublisher.publish("/topic/" + topic, payload);
        try {
            JsonNode node = objectMapper.readTree(payload);

            String compositeId = readCompositeId(node);
            if (compositeId == null) {
                compositeId = deriveCompositeIdFromTopic(topic);
            }
            if (compositeId == null || compositeId.isBlank()) {
                if (topic == null || !topic.contains("/")) {
                    log.debug("Ignoring MQTT message on topic {} without composite_id", topic);
                } else {
                    log.warn("MQTT parse/handle failed on topic {}: missing composite_id", topic);
                }
                return;
            }

            deviceProvisionService.ensureDevice(compositeId, topic);
            recordService.saveRecord(compositeId, node);
            lastSeen.update(compositeId);
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

    private enum TopicPrefix {
        GROW("growSensors"),
        WATER("waterTank");

        private final Pattern pattern;

        TopicPrefix(String prefix) {
            this.pattern = Pattern.compile("^" + prefix + "/(S\\d+)/(L\\d+)/([^/]+)$");
        }
    }

    private static String deriveCompositeId(Matcher matcher) {
        return matcher.group(1) + "-" + matcher.group(2) + "-" + matcher.group(3);
    }

    private static String deriveCompositeIdFromSupportedPrefixes(String topic) {
        if (topic == null) return null;
        for (TopicPrefix prefix : TopicPrefix.values()) {
            Matcher matcher = prefix.pattern.matcher(topic);
            if (matcher.find()) {
                return deriveCompositeId(matcher);
            }
        }
        return null;
    }

    private String deriveCompositeIdFromTopic(String topic) {
        return deriveCompositeIdFromSupportedPrefixes(topic);
    }

}

