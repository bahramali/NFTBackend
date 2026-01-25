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
        assertEquals("S01-L04-LAYER_S01_R01_L04_01", result.compositeId());
    }

    @Test
    void rejectsUnsupportedKinds() {
        String topic = "hydroleaf/v1/S01/R01/L04/LAYER_S01_R01_L04_01/config";

        assertFalse(MqttTopicParser.parse(topic).isPresent());
    }
}
