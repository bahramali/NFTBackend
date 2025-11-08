package se.hydroleaf.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import se.hydroleaf.service.RecordService;
import se.hydroleaf.service.WaterFlowStatusService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MqttMessageHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RecordService recordService = mock(RecordService.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final TopicPublisher topicPublisher = new TopicPublisher(true, messagingTemplate);
    private final WaterFlowStatusService waterFlowStatusService = mock(WaterFlowStatusService.class);
    private final MqttMessageHandler handler = new MqttMessageHandler(objectMapper, recordService, topicPublisher, waterFlowStatusService);

    @Test
    void handleWaterFlowTimestampWithoutTimezoneAssumesUtc() {
        String payload = "{\"status\":\"ON\",\"timestamp\":\"2025-11-08T19:38:49\",\"source\":\"sensor\"}";

        handler.handle("water_flow", payload);

        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(waterFlowStatusService).recordStatus(eq("ON"), instantCaptor.capture(), eq("sensor"));

        Instant expected = LocalDateTime.of(2025, 11, 8, 19, 38, 49).toInstant(ZoneOffset.UTC);
        assertEquals(expected, instantCaptor.getValue());
    }
}
