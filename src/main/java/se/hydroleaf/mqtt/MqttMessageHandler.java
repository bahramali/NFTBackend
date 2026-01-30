package se.hydroleaf.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import se.hydroleaf.service.RecordService;
import se.hydroleaf.service.WaterFlowStatusService;
import se.hydroleaf.service.DeviceStatusEventService;
import se.hydroleaf.model.TopicName;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
@Component
public class MqttMessageHandler {

    private final ObjectMapper objectMapper;
    private final RecordService recordService;
    private final TopicPublisher topicPublisher;
    private final WaterFlowStatusService waterFlowStatusService;
    private final DeviceStatusEventService deviceStatusEventService;

    public MqttMessageHandler(ObjectMapper objectMapper,
                              RecordService recordService,
                              TopicPublisher topicPublisher,
                              WaterFlowStatusService waterFlowStatusService,
                              DeviceStatusEventService deviceStatusEventService) {
        this.objectMapper = objectMapper;
        this.recordService = recordService;
        this.topicPublisher = topicPublisher;
        this.waterFlowStatusService = waterFlowStatusService;
        this.deviceStatusEventService = deviceStatusEventService;
    }

    public void handle(String topic, String payload) {
        try {
            int payloadLength = payload != null ? payload.length() : 0;
            log.debug("MQTT received message (topic={}, payloadLength={})", topic, payloadLength);
            JsonNode node = objectMapper.readTree(payload);
            MqttTopicParser.ParsedTopic parsedTopic = MqttTopicParser.parse(topic).orElse(null);
            String compositeId = parsedTopic != null ? parsedTopic.compositeId() : readCompositeId(node);
            String messageKind = parsedTopic != null ? parsedTopic.kind() : readText(node, "kind");
            if (messageKind == null && topic != null) {
                String trimmedTopic = topic.trim();
                if (trimmedTopic.equalsIgnoreCase("event") || trimmedTopic.equalsIgnoreCase("/event")
                        || trimmedTopic.toLowerCase().endsWith("/event")) {
                    messageKind = "event";
                }
            }
            if (parsedTopic != null) {
                String payloadCompositeId = readCompositeId(node);
                if (payloadCompositeId != null && !payloadCompositeId.isBlank()
                        && !payloadCompositeId.equals(compositeId)) {
                    log.warn("MQTT payload composite_id {} does not match topic-derived {}", payloadCompositeId, compositeId);
                }
                log.debug("MQTT parsed topic site={} rack={} layer={} deviceId={} kind={} compositeId={}",
                        parsedTopic.site(), parsedTopic.rack(), parsedTopic.layer(), parsedTopic.deviceId(),
                        parsedTopic.kind(), parsedTopic.compositeId());
            }

            if (topic != null && !topic.isBlank()) {
                topicPublisher.publish("/topic/" + topic, payload, compositeId, messageKind);
            }

            if (parsedTopic != null) {
                JsonNode envelopePayload = buildEnvelopePayload(parsedTopic, node);
                String aggregateTopic = "/topic/hydroleaf/" + parsedTopic.kind();
                log.debug("MQTT publishing aggregate destination={}", aggregateTopic);
                topicPublisher.publish(aggregateTopic, envelopePayload, parsedTopic.compositeId(), parsedTopic.kind());
                String rackTopic = String.format("/topic/hydroleaf/rack/%s/%s", parsedTopic.rack(), parsedTopic.kind());
                log.debug("MQTT publishing rack destination={}", rackTopic);
                topicPublisher.publish(rackTopic, envelopePayload, parsedTopic.compositeId(), parsedTopic.kind());
            }

            if (isWaterFlowTopic(topic)) {
                handleWaterFlow(node);
                return;
            }

            TopicName topicName = parsedTopic != null ? null : TopicName.fromMqttTopic(topic);

            if (messageKind != null && "event".equalsIgnoreCase(messageKind)) {
                log.info("MQTT event received (topic={}, compositeId={})", topic, compositeId);
                if (parsedTopic == null) {
                    topicPublisher.publish("/topic/hydroleaf/event", payload, compositeId, messageKind);
                }
            }

            if (compositeId == null || compositeId.isBlank()) {
                if (topic == null || !topic.contains("/")) {
                    log.warn("Ignoring MQTT message on topic {} without composite_id", topic);
                } else {
                    log.warn("MQTT parse/handle failed on topic {}: missing composite_id", topic);
                }
                return;
            }

            if (messageKind != null && "status".equalsIgnoreCase(messageKind)) {
                String statusValue = readText(node, "status", "value");
                Instant statusTime = parseTimestamp(node.path("timestamp"), node.path("status_time"), node.path("ts"));
                deviceStatusEventService.recordStatus(compositeId, statusValue, statusTime);
                return;
            }
            if (messageKind != null && "event".equalsIgnoreCase(messageKind)) {
                Instant eventTime = parseTimestamp(node.path("timestamp"), node.path("event_time"), node.path("ts"));
                String level = readText(node, "level");
                String code = readText(node, "code");
                String msg = readText(node, "msg", "message");
                deviceStatusEventService.recordEvent(compositeId, eventTime, level, code, msg, payload);
                return;
            }

            recordService.saveRecord(compositeId, node, topicName, topic, parsedTopic);
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

        String status = readText(node, "value", "status");
        String source = readText(node, "sensorName", "source");
        String sensorType = readText(node, "sensorType");

        Instant timestamp = parseTimestamp(node.path("timestamp"));

        waterFlowStatusService.recordStatus(status, timestamp, source, sensorType);
    }

    private static String readText(JsonNode node, String... fieldNames) {
        if (node == null || fieldNames == null) {
            return null;
        }

        for (String fieldName : fieldNames) {
            if (fieldName == null) {
                continue;
            }

            JsonNode field = node.get(fieldName);
            if (field != null && field.isTextual()) {
                return field.asText();
            }
        }

        return null;
    }

    private Instant parseTimestamp(JsonNode... timestampNodes) {
        if (timestampNodes == null) {
            return null;
        }
        for (JsonNode tsNode : timestampNodes) {
            if (tsNode == null || tsNode.isMissingNode() || tsNode.isNull()) {
                continue;
            }
            if (tsNode.isNumber()) {
                return Instant.ofEpochMilli(tsNode.asLong());
            }
            if (!tsNode.isTextual()) {
                continue;
            }

            String tsValue = tsNode.asText();
            try {
                return Instant.parse(tsValue);
            } catch (DateTimeParseException ignored) {
                // Fall through to attempt parsing without timezone information.
            }

            try {
                LocalDateTime dateTime = LocalDateTime.parse(tsValue, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return dateTime.toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException ex) {
                log.warn("Unable to parse timestamp '{}' in payload", tsValue, ex);
                return null;
            }
        }
        return null;
    }

    private static String readCompositeId(JsonNode n) {
        if (n == null) return null;
        if (n.hasNonNull("composite_id")) return n.get("composite_id").asText();
        if (n.hasNonNull("compositeId")) return n.get("compositeId").asText();
        return null;
    }

    private JsonNode buildEnvelopePayload(MqttTopicParser.ParsedTopic parsedTopic, JsonNode payload) {
        var envelope = objectMapper.createObjectNode();
        envelope.put("schemaVersion", 2);
        envelope.put("kind", parsedTopic.kind());
        envelope.put("deviceId", parsedTopic.deviceId());
        String timestampText = resolveTimestampText(payload);
        if (timestampText != null) {
            envelope.put("timestamp", timestampText);
        } else {
            envelope.putNull("timestamp");
        }
        envelope.put("siteId", parsedTopic.site());
        envelope.put("rackId", parsedTopic.rack());

        String nodeType = extractNodeType(parsedTopic.deviceId());
        envelope.put("nodeType", nodeType);
        envelope.put("nodeId", resolveNodeId(nodeType, parsedTopic));
        envelope.put("nodeInstance", resolveNodeInstance(nodeType, parsedTopic.deviceId()));

        envelope.set("payload", normalizePayload(payload));
        return envelope;
    }

    private String resolveTimestampText(JsonNode payload) {
        if (payload == null) {
            return null;
        }
        JsonNode tsNode = payload.path("timestamp");
        Instant instant = parseTimestamp(tsNode);
        if (instant != null) {
            return instant.toString();
        }
        if (tsNode.isTextual()) {
            return tsNode.asText();
        }
        return null;
    }

    private static String extractNodeType(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return null;
        }
        int underscoreIndex = deviceId.indexOf('_');
        String prefix = underscoreIndex > 0 ? deviceId.substring(0, underscoreIndex) : deviceId;
        return prefix.toUpperCase();
    }

    private static String resolveNodeId(String nodeType, MqttTopicParser.ParsedTopic parsedTopic) {
        if ("LAYER".equalsIgnoreCase(nodeType)) {
            return parsedTopic.layer();
        }
        if ("TANK".equalsIgnoreCase(nodeType)) {
            return extractSuffixIndex(parsedTopic.deviceId());
        }
        return parsedTopic.deviceId();
    }

    private static int resolveNodeInstance(String nodeType, String deviceId) {
        if ("LAYER".equalsIgnoreCase(nodeType)) {
            Integer parsed = extractNumericSuffix(deviceId);
            if (parsed != null) {
                return parsed;
            }
        }
        return 1;
    }

    private static String extractSuffixIndex(String deviceId) {
        if (deviceId == null) {
            return null;
        }
        int lastUnderscore = deviceId.lastIndexOf('_');
        if (lastUnderscore < 0 || lastUnderscore >= deviceId.length() - 1) {
            return deviceId;
        }
        return deviceId.substring(lastUnderscore + 1);
    }

    private static Integer extractNumericSuffix(String deviceId) {
        if (deviceId == null) {
            return null;
        }
        String suffix = extractSuffixIndex(deviceId);
        try {
            return Integer.parseInt(suffix);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private JsonNode normalizePayload(JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            return payload;
        }
        var cleaned = payload.deepCopy();
        cleaned.remove("site");
        cleaned.remove("rack");
        cleaned.remove("layer");
        cleaned.remove("deviceId");
        cleaned.remove("timestamp");
        cleaned.remove("siteId");
        cleaned.remove("rackId");
        cleaned.remove("layerId");
        cleaned.remove("systemId");
        cleaned.remove("system");
        cleaned.remove("compositeId");
        cleaned.remove("composite_id");
        cleaned.remove("kind");
        return cleaned;
    }

}
