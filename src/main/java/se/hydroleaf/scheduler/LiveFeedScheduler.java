package se.hydroleaf.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import se.hydroleaf.dto.snapshot.LiveNowSnapshot;
import se.hydroleaf.mqtt.TopicPublisher;
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
    private final TopicPublisher topicPublisher;
    private final ConcurrentHashMap<String, Instant> lastSeen;
    private final ObjectMapper objectMapper;
    private Instant lastInvocation;
    public LiveFeedScheduler(StatusService statusService,
                             TopicPublisher topicPublisher,
                             ConcurrentHashMap<String, Instant> lastSeen,
                             ObjectMapper objectMapper) {
        this.statusService = statusService;
        this.topicPublisher = topicPublisher;
        this.lastSeen = lastSeen;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedRateString = "${livefeed.rate:2000}")
    public void sendLiveNow() {
        Instant start = Instant.now();
        if (log.isDebugEnabled()) {
            if (lastInvocation != null) {
                long sinceLast = Duration.between(lastInvocation, start).toMillis();
                log.debug("sendLiveNow invoked at {} ({} ms since last)", start, sinceLast);
            } else {
                log.debug("sendLiveNow invoked at {}", start);
            }
        }
        lastInvocation = start;

        LiveNowSnapshot snapshot = statusService.getLiveNowSnapshot();
        Instant afterSnapshot = Instant.now();
        log.debug("getLiveNowSnapshot took {} ms", Duration.between(start, afterSnapshot).toMillis());

        String payload;
        try {
            payload = objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize LiveNowSnapshot", e);
            return;
        }
        try {
            Instant sendStart = Instant.now();
            topicPublisher.publish("/topic/live_now", payload);
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
