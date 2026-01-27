package se.hydroleaf.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.hydroleaf.service.DeviceStatusEventService;
import se.hydroleaf.service.RecordService;
import se.hydroleaf.service.WaterFlowStatusService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MqttMessageHandlerHydroleafTopicTest {

    @Mock
    RecordService recordService;
    @Mock
    TopicPublisher topicPublisher;
    @Mock
    WaterFlowStatusService waterFlowStatusService;
    @Mock
    DeviceStatusEventService deviceStatusEventService;
    ObjectMapper objectMapper;
    MqttMessageHandler handler;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        handler = new MqttMessageHandler(objectMapper, recordService, topicPublisher, waterFlowStatusService, deviceStatusEventService);
    }

    @Test
    void hydroleafTopicUsesCompositeIdFromTopic() {
        String topic = "hydroleaf/v1/S01/R01/L04/LAYER_S01_R01_L04_01/telemetry";
        String payload = """
                {
                  "timestamp": "2025-01-01T00:00:00Z",
                  "sensors": [
                    {
                      "sensorType": "temperature",
                      "value": 20.0
                    }
                  ]
                }
                """;

        handler.handle(topic, payload);

        verify(recordService).saveRecord(eq("S01-R01-L04-LAYER_S01_R01_L04_01"), any(), isNull(), eq(topic), any());
        verify(topicPublisher).publish(eq("/topic/" + topic), eq(payload));
    }
}
