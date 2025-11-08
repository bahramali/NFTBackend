package se.hydroleaf.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import se.hydroleaf.service.RecordService;
import se.hydroleaf.service.WaterFlowStatusService;
import se.hydroleaf.model.TopicName;

import java.time.Instant;
import java.time.format.DateTimeParseException;

@Slf4j
@Component
public class MqttMessageHandler {

    private final ObjectMapper objectMapper;
    private final RecordService recordService;
    private final TopicPublisher topicPublisher;
    private final WaterFlowStatusService waterFlowStatusService;

    public MqttMessageHandler(ObjectMapper objectMapper,
                              RecordService recordService,
                              TopicPublisher topicPublisher,
                              WaterFlowStatusService waterFlowStatusService) {
        this.objectMapper = objectMapper;
        this.recordService = recordService;
        this.topicPublisher = topicPublisher;
        this.waterFlowStatusService = waterFlowStatusService;
    }

    public void handle(String topic, String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String compositeId = readCompositeId(node);

            if (topic != null && !topic.isBlank()) {
                topicPublisher.publish("/topic/" + topic, payload);
            }

            if (isWaterFlowTopic(topic)) {
                handleWaterFlow(node);
                return;
            }

            if (compositeId == null || compositeId.isBlank()) {
                if (topic == null || !topic.contains("/")) {
                    log.warn("Ignoring MQTT message on topic {} without composite_id", topic);
                } else {
                    log.warn("MQTT parse/handle failed on topic {}: missing composite_id", topic);
                }
                return;
            }

            TopicName topicName = TopicName.fromMqttTopic(topic);

            recordService.saveRecord(compositeId, node, topicName);
        } catch (Exception ex) {
            log.error("MQTT handle error for topic {}: {}", topic, ex.getMessage(), ex);
        }
    }

    private static boolean isWaterFlowTopic(String topic) {
        if (topic == null) {
            return false;
        }
        String trimmed = topic.trim();
        return "water_flow".equalsIgnoreCase(trimmed) || trimmed.toLowerCase().startsWith("water_flow/");
    }

    private void handleWaterFlow(JsonNode node) {
        if (node == null) {
            return;
        }

        String status = node.path("status").asText(null);
        String source = node.path("source").asText(null);

        Instant timestamp = null;
        JsonNode tsNode = node.path("timestamp");
        if (tsNode.isTextual()) {
            String tsValue = tsNode.asText();
            try {
                timestamp = Instant.parse(tsValue);
            } catch (DateTimeParseException ex) {
                log.warn("Unable to parse timestamp '{}' in water_flow payload", tsValue, ex);
            }
        }

        waterFlowStatusService.recordStatus(status, timestamp, source);
    }

    private static String readCompositeId(JsonNode n) {
        if (n == null) return null;
        if (n.hasNonNull("composite_id")) return n.get("composite_id").asText();
        if (n.hasNonNull("compositeId")) return n.get("compositeId").asText();
        return null;
    }

}

