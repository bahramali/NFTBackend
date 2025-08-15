package se.hydroleaf.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import se.hydroleaf.dto.*;
import se.hydroleaf.service.StatusService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveFeedSchedulerTest {

    @Mock
    private StatusService statusService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void sendLiveNowPublishesSnapshotWithCategorizedDto() {
        LiveNowSnapshot snapshot = new LiveNowSnapshot(
                Map.of("S1",
                        new SystemSnapshot(
                                java.time.Instant.now(),
                                new LayerActuatorStatus(new StatusAverageResponse(1.0,1L)),
                                new WaterTankSummary(
                                        new StatusAverageResponse(5.0,1L),
                                        new StatusAverageResponse(6.0,1L),
                                        new StatusAverageResponse(7.0,1L),
                                        new StatusAverageResponse(8.0,1L)
                                ),
                                new GrowSensorSummary(
                                        new StatusAverageResponse(2.0,1L),
                                        new StatusAverageResponse(3.0,1L),
                                        new StatusAverageResponse(4.0,1L)
                                )
                        ))
        );
        when(statusService.getLiveNowSnapshot()).thenReturn(snapshot);

        LiveFeedScheduler scheduler = new LiveFeedScheduler(true, statusService, messagingTemplate, new ConcurrentHashMap<>());
        scheduler.sendLiveNow();

        ArgumentCaptor<LiveNowSnapshot> captor = ArgumentCaptor.forClass(LiveNowSnapshot.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/live_now"), captor.capture());

        LiveNowSnapshot sent = captor.getValue();
        assertEquals(6.0, sent.systems().get("S1").water().dissolvedOxygen().average());
        assertEquals(1.0, sent.systems().get("S1").actuators().airPump().average());
    }
}

