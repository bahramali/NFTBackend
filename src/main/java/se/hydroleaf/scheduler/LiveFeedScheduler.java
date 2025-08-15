package se.hydroleaf.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    private final boolean publishEnabled;
    private final StatusService statusService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConcurrentHashMap<String, Instant> lastSeen;

    public LiveFeedScheduler(@Value("${mqtt.publishEnabled:true}") boolean publishEnabled,
                             StatusService statusService,
                             SimpMessagingTemplate messagingTemplate,
                             ConcurrentHashMap<String, Instant> lastSeen) {
        this.publishEnabled = publishEnabled;
        this.statusService = statusService;
        this.messagingTemplate = messagingTemplate;
        this.lastSeen = lastSeen;
    }

    @Scheduled(fixedRate = 2000)
    public void sendLiveNow() {
        if (!publishEnabled){
            log.info("Local not publish to mqtt");
            return; // block in local
        }
        try {
            LiveNowSnapshot snapshot = statusService.getLiveNowSnapshot();
            log.info("sending to /topic/live_now: {}", snapshot);
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
