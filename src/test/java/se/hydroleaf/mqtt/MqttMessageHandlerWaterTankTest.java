package se.hydroleaf.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.hydroleaf.service.RecordService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MqttMessageHandlerWaterTankTest {

    @Mock
    RecordService recordService;
    @Mock
    TopicPublisher topicPublisher;
    ObjectMapper objectMapper;
    MqttMessageHandler handler;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        handler = new MqttMessageHandler(objectMapper, recordService, topicPublisher);
    }

    @Test
    void waterTankTopicWithoutCompositeId_isProcessed() {
        String topic = "waterTank/S01/L01/probe1";
        String payload = "{}";

        handler.handle(topic, payload);

        verify(recordService).saveRecord(eq("S01-L01-probe1"), any());
        verify(topicPublisher).publish(eq("/topic/" + topic), eq(payload));
    }

    @Test
    void waterTankBaseTopicWithoutCompositeId_isIgnored() {
        String topic = "waterTank";
        String payload = "{}";

        handler.handle(topic, payload);

        verifyNoInteractions(recordService, topicPublisher);
    }

    @Test
    void invalidJson_isDiscardedWithoutPublishing() {
        String topic = "waterTank/S01/L01/probe1";
        String payload = "{"; // malformed JSON

        handler.handle(topic, payload);

        verifyNoInteractions(recordService, topicPublisher);
    }
}
