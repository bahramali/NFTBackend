package se.hydroleaf.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.hydroleaf.service.RecordService;
import se.hydroleaf.model.TopicName;
import se.hydroleaf.service.DeviceStatusEventService;
import se.hydroleaf.service.WaterFlowStatusService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MqttMessageHandlerWaterTankTest {

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
    void waterTankTopicWithoutCompositeId_isProcessed() {
        String topic = "waterTank";
        String payload = """
                {
                    "system": "S01",
                    "deviceId": "G02",
                    "location": "L01",
                    "compositeId": "S01-R01-L01-G02",
                    "timestamp": "2025-08-09T11:41:18Z",
                      "sensors": [
                        {
                          "sensorName": "HailegeTDS",
                          "valueType": "tds",
                          "value": 1003.116,
                          "unit": "ppm"
                        }
                      ],
                      "health": {
                        "HailegeTDS": true,
                        "DS18B20": true,
                        "DFROBOT": true
                      }
                }
                """;

        handler.handle(topic, payload);

        verify(recordService).saveRecord(eq("S01-R01-L01-G02"), any(), eq(TopicName.waterTank), eq(topic), any());
        verify(topicPublisher).publish(eq("/topic/" + topic), eq(payload), eq("S01-R01-L01-G02"), isNull());
    }

    @Test
    void waterTankBaseTopicWithoutCompositeId_isIgnored() {
        String topic = "waterTank";
        String payload = "{}";

        handler.handle(topic, payload);

        verify(topicPublisher).publish(eq("/topic/" + topic), eq(payload), isNull(), isNull());
        verifyNoInteractions(recordService);
    }

    @Test
    void invalidJson_isDiscardedWithoutPublishing() {
        String topic = "waterTank/S01/L01/probe1";
        String payload = "{"; // malformed JSON

        handler.handle(topic, payload);

        verifyNoInteractions(recordService, topicPublisher);
    }
}
