package se.hydroleaf.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import se.hydroleaf.service.RecordService;
import se.hydroleaf.model.TopicName;

@Slf4j
@Component
public class MqttMessageHandler {

    private final ObjectMapper objectMapper;
    private final RecordService recordService;
    private final TopicPublisher topicPublisher;

    public MqttMessageHandler(ObjectMapper objectMapper,
                              RecordService recordService,
                              TopicPublisher topicPublisher) {
        this.objectMapper = objectMapper;
        this.recordService = recordService;
        this.topicPublisher = topicPublisher;
    }

    public void handle(String topic, String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String compositeId = readCompositeId(node);

            if (compositeId == null || compositeId.isBlank()) {
                if (topic == null || !topic.contains("/")) {
                    log.warn("Ignoring MQTT message on topic {} without composite_id", topic);
                } else {
                    log.warn("MQTT parse/handle failed on topic {}: missing composite_id", topic);
                }
                return;
            }
            if(TopicName.germinationTopic.name().equals(topic)){
                log.info("topic: {}, message: {}", topic, payload);
            }
            topicPublisher.publish("/topic/" + topic, payload);

            TopicName topicName = null;
            if (topic != null) {
                String prefix = topic.contains("/") ? topic.substring(0, topic.indexOf('/')) : topic;
                try {
                    topicName = TopicName.valueOf(prefix);
                } catch (IllegalArgumentException ignore) {
                }
            }

            recordService.saveRecord(compositeId, node, topicName);
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

}

