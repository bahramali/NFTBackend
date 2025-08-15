package se.hydroleaf.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import se.hydroleaf.service.DeviceProvisionService;
import se.hydroleaf.service.RecordService;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MqttMessageHandlerWaterTankTest {

    @Mock
    RecordService recordService;
    @Mock
    SimpMessagingTemplate messagingTemplate;
    @Mock
    DeviceProvisionService deviceProvisionService;

    ObjectMapper objectMapper;
    ConcurrentHashMap<String, Instant> lastSeen;
    MqttMessageHandler handler;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        lastSeen = new ConcurrentHashMap<>();
        handler = new MqttMessageHandler(false, objectMapper, recordService, messagingTemplate, deviceProvisionService, lastSeen);
    }

    @Test
    void waterTankTopicWithoutCompositeId_isProcessed() {
        String topic = "waterTank/S01/L01/probe1";
        String payload = "{}";

        handler.handle(topic, payload);

        verify(deviceProvisionService).ensureDevice(eq("S01-L01-probe1"), eq(topic));
        verify(recordService).saveRecord(eq("S01-L01-probe1"), any());
        assertTrue(lastSeen.containsKey("S01-L01-probe1"));
    }

    @Test
    void waterTankBaseTopicWithoutCompositeId_isIgnored() {
        String topic = "waterTank";
        String payload = "{}";

        handler.handle(topic, payload);

        verifyNoInteractions(deviceProvisionService, recordService);
        assertTrue(lastSeen.isEmpty());
    }
}
