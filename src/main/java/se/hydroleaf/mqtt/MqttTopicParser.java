package se.hydroleaf.mqtt;

import java.util.Locale;
import java.util.Optional;

public final class MqttTopicParser {

    private static final String PREFIX = "hydroleaf/v1/";

    private MqttTopicParser() {
    }

    public static Optional<ParsedTopic> parse(String topic) {
        if (topic == null) {
            return Optional.empty();
        }

        String trimmed = topic.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }

        if (!trimmed.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            return Optional.empty();
        }

        String remainder = trimmed.substring(PREFIX.length());
        String[] parts = remainder.split("/");
        if (parts.length < 5) {
            return Optional.empty();
        }

        String site = parts[0];
        String rack = parts[1];
        String layer = parts[2];
        String deviceId = parts[3];
        String kind = parts[4];

        if (isBlank(site) || isBlank(rack) || isBlank(layer) || isBlank(deviceId) || isBlank(kind)) {
            return Optional.empty();
        }

        String normalizedKind = kind.trim().toLowerCase(Locale.ROOT);
        if (!"telemetry".equals(normalizedKind) && !"status".equals(normalizedKind)) {
            return Optional.empty();
        }

        return Optional.of(new ParsedTopic(site, rack, layer, deviceId, normalizedKind));
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record ParsedTopic(String site, String rack, String layer, String deviceId, String kind) {
        public String compositeId() {
            return String.format("%s-%s-%s", site, layer, deviceId);
        }
    }
}
