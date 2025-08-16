package se.hydroleaf.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import se.hydroleaf.dto.*;
import se.hydroleaf.service.StatusService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveFeedSchedulerTest {

    @Mock
    private StatusService statusService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void sendLiveNowPublishesSnapshotWithLayers() throws Exception {
        SystemSnapshot.LayerSnapshot layerSnapshot = new SystemSnapshot.LayerSnapshot(
                "L1",
                java.time.Instant.now(),
                  new ActuatorStatusSummary(new StatusAverageResponse(1.0, "status", 1L)),
                new WaterTankSummary(
                        new StatusAverageResponse(5.0, "°C", 1L),
                        new StatusAverageResponse(6.0, "mg/L", 1L),
                        new StatusAverageResponse(7.0, "pH", 1L),
                        new StatusAverageResponse(8.0, "mS/cm", 1L),
                        new StatusAverageResponse(9.0, "ppm", 1L)
                ),
                new GrowSensorSummary(
                        new StatusAverageResponse(2.0, "lux", 1L),
                        new StatusAverageResponse(3.0, "%", 1L),
                        new StatusAverageResponse(4.0, "°C", 1L)
                )
        );
        SystemSnapshot systemSnapshot = new SystemSnapshot(
                java.time.Instant.now(),
                  new ActuatorStatusSummary(new StatusAverageResponse(1.0, "status", 1L)),
                new WaterTankSummary(
                        new StatusAverageResponse(5.0, "°C", 1L),
                        new StatusAverageResponse(6.0, "mg/L", 1L),
                        new StatusAverageResponse(7.0, "pH", 1L),
                        new StatusAverageResponse(8.0, "mS/cm", 1L),
                        new StatusAverageResponse(9.0, "ppm", 1L)
                ),
                new GrowSensorSummary(
                        new StatusAverageResponse(2.0, "lux", 1L),
                        new StatusAverageResponse(3.0, "%", 1L),
                        new StatusAverageResponse(4.0, "°C", 1L)
                ),
                List.of(layerSnapshot)
        );
        LiveNowSnapshot snapshot = new LiveNowSnapshot(
                Map.of("S1", systemSnapshot)
        );
        when(statusService.getLiveNowSnapshot()).thenReturn(snapshot);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        LiveFeedScheduler scheduler = new LiveFeedScheduler(true, statusService, messagingTemplate, new ConcurrentHashMap<>(), mapper);
        scheduler.sendLiveNow();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/live_now"), captor.capture());

        LiveNowSnapshot sent = mapper.readValue(captor.getValue(), LiveNowSnapshot.class);
        assertEquals(1, sent.systems().get("S1").layers().size());
        SystemSnapshot system = sent.systems().get("S1");
        SystemSnapshot.LayerSnapshot sentLayer = system.layers().get(0);
        assertEquals(6.0, sentLayer.water().dissolvedOxygen().average());
        assertEquals(1.0, sentLayer.actuators().airPump().average());
        assertNotNull(sentLayer.lastUpdate());
        assertEquals(1.0, system.actuators().airPump().average());
    }

    @Test
    void continuesAfterSerializationFailure() throws Exception {
        LiveNowSnapshot snapshot = new LiveNowSnapshot(Map.of());
        when(statusService.getLiveNowSnapshot()).thenReturn(snapshot);

        ObjectMapper mapper = mock(ObjectMapper.class);
        when(mapper.writeValueAsString(any()))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("boom") {})
                .thenReturn("{}");

        LiveFeedScheduler scheduler = new LiveFeedScheduler(true, statusService, messagingTemplate, new ConcurrentHashMap<>(), mapper);

        assertDoesNotThrow(scheduler::sendLiveNow);
        scheduler.sendLiveNow();

        verify(statusService, times(2)).getLiveNowSnapshot();
        verify(messagingTemplate).convertAndSend("/topic/live_now", "{}");
    }
}

