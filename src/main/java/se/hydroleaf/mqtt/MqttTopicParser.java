package se.hydroleaf.mqtt;

import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MqttTopicParser {

    private static final Logger log = LoggerFactory.getLogger(MqttTopicParser.class);
    private static final int EXPECTED_PARTS = 7;

    private MqttTopicParser() {
    }

    public static Optional<ParsedTopic> parse(String topic) {
        if (topic == null) {
            return warnAndEmpty("topic is null", "null");
        }

        String trimmed = topic.trim();
        if (trimmed.isEmpty()) {
            return warnAndEmpty("topic is empty", topic);
        }

        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }

        String[] parts = trimmed.split("/");
        if (parts.length != EXPECTED_PARTS) {
            return warnAndEmpty("expected 7 segments but got " + parts.length, trimmed);
        }

        if (!"hydroleaf".equals(parts[0]) || !"v1".equals(parts[1])) {
            return warnAndEmpty("unexpected prefix (expected hydroleaf/v1)", trimmed);
        }

        String site = parts[2];
        String rack = parts[3];
        String layer = parts[4];
        String deviceId = parts[5];
        String kind = parts[6];

        if (isBlank(site) || isBlank(rack) || isBlank(layer) || isBlank(deviceId) || isBlank(kind)) {
            return warnAndEmpty("blank segment", trimmed);
        }

        String normalizedKind = kind.trim().toLowerCase(Locale.ROOT);
        if (!"telemetry".equals(normalizedKind)
                && !"status".equals(normalizedKind)
                && !"event".equals(normalizedKind)) {
            return warnAndEmpty("unsupported kind: " + kind, trimmed);
        }

        return Optional.of(new ParsedTopic(site, rack, layer, deviceId, normalizedKind));
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static Optional<ParsedTopic> warnAndEmpty(String reason, String topic) {
        log.warn("Unparseable MQTT topic (reason={}): {}", reason, topic);
        return Optional.empty();
    }

    public record ParsedTopic(String site, String rack, String layer, String deviceId, String kind) {
        public String compositeId() {
            return String.format("%s-%s-%s-%s", site, rack, layer, deviceId);
        }
    }
}
