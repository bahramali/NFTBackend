package se.hydroleaf.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;
    private Instant lastInvocation;
    public LiveFeedScheduler(@Value("${mqtt.publishEnabled:true}") boolean publishEnabled,
                             StatusService statusService,
                             SimpMessagingTemplate messagingTemplate,
                             ConcurrentHashMap<String, Instant> lastSeen,
                             ObjectMapper objectMapper) {
        this.publishEnabled = publishEnabled;
        this.statusService = statusService;
        this.messagingTemplate = messagingTemplate;
        this.lastSeen = lastSeen;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedRateString = "${livefeed.rate:2000}")
    public void sendLiveNow() {
        Instant start = Instant.now();
        if (lastInvocation != null) {
            long sinceLast = Duration.between(lastInvocation, start).toMillis();
            log.info("sendLiveNow invoked at {} ({} ms since last)", start, sinceLast);
        } else {
            log.info("sendLiveNow invoked at {}", start);
        }
        lastInvocation = start;

        LiveNowSnapshot snapshot = statusService.getLiveNowSnapshot();
        Instant afterSnapshot = Instant.now();
        log.debug("getLiveNowSnapshot took {} ms", Duration.between(start, afterSnapshot).toMillis());

        String payload = "";
        try {
            payload = objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        if (!publishEnabled) {
            log.info("Should publish to /topic/live_now with payload: {}", payload);
            return; // block in local
        }
        try {
            Instant sendStart = Instant.now();
            messagingTemplate.convertAndSend("/topic/live_now", payload);
            log.debug("convertAndSend took {} ms", Duration.between(sendStart, Instant.now()).toMillis());
        } catch (Exception e) {
            log.warn("sendLiveNow failed: {}", e.getMessage());
        }

        log.debug("sendLiveNow completed in {} ms", Duration.between(start, Instant.now()).toMillis());
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
