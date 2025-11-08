package se.hydroleaf.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.hydroleaf.model.TopicName;
import se.hydroleaf.service.RecordService;
import se.hydroleaf.service.WaterFlowStatusService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MqttMessageHandlerGerminationTopicTest {

    @Mock
    RecordService recordService;
    @Mock
    TopicPublisher topicPublisher;
    @Mock
    WaterFlowStatusService waterFlowStatusService;
    ObjectMapper objectMapper;
    MqttMessageHandler handler;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        handler = new MqttMessageHandler(objectMapper, recordService, topicPublisher, waterFlowStatusService);
    }

    @Test
    void germinationTopicMessage_isProcessedAndPublished() {
        String topic = "germinationTopic/S01/L02/sensor";
        String payload = """
                {
                  "composite_id": "S01-L02-G03",
                  "timestamp": "2025-01-01T00:00:00Z",
                  "sensors": [
                    {
                      "sensorType": "humidity",
                      "value": 42.5
                    }
                  ]
                }
                """;

        handler.handle(topic, payload);

        verify(recordService).saveRecord(eq("S01-L02-G03"), any(), eq(TopicName.germinationTopic));
        verify(topicPublisher).publish(eq("/topic/" + topic), eq(payload));
    }

    @Test
    void topicPrefixIsResolvedCaseInsensitively() {
        String topic = "GeRmInAtIoNToPiC/device";
        String payload = """
                {
                  "compositeId": "S01-L02-G03",
                  "timestamp": "2025-01-01T00:00:00Z",
                  "sensors": []
                }
                """;

        handler.handle(topic, payload);

        verify(recordService).saveRecord(eq("S01-L02-G03"), any(), eq(TopicName.germinationTopic));
        verify(topicPublisher).publish(eq("/topic/" + topic), eq(payload));
    }
}
