package se.hydroleaf.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import se.hydroleaf.dto.LiveNowSnapshot;
import se.hydroleaf.service.StatusService;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Periodic tasks related to live device feed.
 */
@Slf4j
@Service
public class LiveFeedScheduler {

    private final StatusService statusService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConcurrentHashMap<String, Instant> lastSeen;

    public LiveFeedScheduler(StatusService statusService,
                              SimpMessagingTemplate messagingTemplate,
                              ConcurrentHashMap<String, Instant> lastSeen) {
        this.statusService = statusService;
        this.messagingTemplate = messagingTemplate;
        this.lastSeen = lastSeen;
    }

    @Scheduled(fixedRate = 2000)
    public void sendLiveNow() {
        try {
            LiveNowSnapshot snapshot = statusService.getLiveNowSnapshot();
            messagingTemplate.convertAndSend("/topic/live_now", snapshot);
        } catch (Exception e) {
            log.warn("sendLiveNow failed: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 10000)
    public void logLaggingDevices() {
        Instant now = Instant.now();
        lastSeen.forEach((id, ts) -> {
            if (Duration.between(ts, now).toSeconds() > 60) {
                log.debug("Device {} no message for >60s (lastSeen={})", id, ts);
            }
        });
    }
}
