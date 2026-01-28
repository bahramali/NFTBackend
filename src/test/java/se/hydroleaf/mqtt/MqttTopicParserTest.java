package se.hydroleaf.mqtt;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MqttTopicParserTest {

    @Test
    void parsesHydroleafV1Topic() {
        String topic = "hydroleaf/v1/S01/R01/L04/LAYER_S01_R01_L04_01/telemetry";

        Optional<MqttTopicParser.ParsedTopic> parsed = MqttTopicParser.parse(topic);

        assertTrue(parsed.isPresent());
        MqttTopicParser.ParsedTopic result = parsed.get();
        assertEquals("S01", result.site());
        assertEquals("R01", result.rack());
        assertEquals("L04", result.layer());
        assertEquals("LAYER_S01_R01_L04_01", result.deviceId());
        assertEquals("telemetry", result.kind());
        assertEquals("S01-R01-L04-LAYER_S01_R01_L04_01", result.compositeId());
    }

    @Test
    void parsesStatusKind() {
        String topic = "hydroleaf/v1/S01/R01/L04/LAYER_S01_R01_L04_01/status";

        Optional<MqttTopicParser.ParsedTopic> parsed = MqttTopicParser.parse(topic);

        assertTrue(parsed.isPresent());
        assertEquals("status", parsed.get().kind());
    }

    @Test
    void rejectsUnsupportedKinds() {
        String topic = "hydroleaf/v1/S01/R01/L04/LAYER_S01_R01_L04_01/config";

        assertFalse(MqttTopicParser.parse(topic).isPresent());
    }

    @Test
    void rejectsMissingKind() {
        String topic = "hydroleaf/v1/S01/R01/L04/LAYER_S01_R01_L04_01";

        assertFalse(MqttTopicParser.parse(topic).isPresent());
    }

    @Test
    void parsesEventKind() {
        String topic = "hydroleaf/v1/S01/R01/L04/LAYER_S01_R01_L04_01/event";

        Optional<MqttTopicParser.ParsedTopic> parsed = MqttTopicParser.parse(topic);

        assertTrue(parsed.isPresent());
        assertEquals("event", parsed.get().kind());
    }

    @Test
    void parsesGerminationTelemetryKind() {
        String topic = "hydroleaf/v1/S01/germination/L00/GER_S01_01/telemetry";

        Optional<MqttTopicParser.ParsedTopic> parsed = MqttTopicParser.parse(topic);

        assertTrue(parsed.isPresent());
        MqttTopicParser.ParsedTopic result = parsed.get();
        assertEquals("S01", result.site());
        assertEquals("germination", result.rack());
        assertEquals("L00", result.layer());
        assertEquals("GER_S01_01", result.deviceId());
        assertEquals("telemetry", result.kind());
    }

    @Test
    void rejectsInvalidPrefix() {
        String topic = "other/v1/S01/R01/L04/LAYER_S01_R01_L04_01/telemetry";

        assertFalse(MqttTopicParser.parse(topic).isPresent());
    }

    @Test
    void rejectsWrongSegmentCount() {
        String tooShort = "hydroleaf/v1/S01/R01/L04/LAYER_S01_R01_L04_01";
        String tooLong = "hydroleaf/v1/S01/R01/L04/LAYER_S01_R01_L04_01/telemetry/extra";

        assertFalse(MqttTopicParser.parse(tooShort).isPresent());
        assertFalse(MqttTopicParser.parse(tooLong).isPresent());
    }
}
