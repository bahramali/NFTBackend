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

/**
 * Periodic tasks related to live device feed.
 */
@Slf4j
@Service
public class LiveFeedScheduler {

    private final StatusService statusService;
    private final TopicPublisher topicPublisher;
    private final LastSeenRegistry lastSeen;
    private final ObjectMapper objectMapper;

    public LiveFeedScheduler(StatusService statusService,
                             TopicPublisher topicPublisher,
                             LastSeenRegistry lastSeen,
                             ObjectMapper objectMapper) {
        this.statusService = statusService;
        this.topicPublisher = topicPublisher;
        this.lastSeen = lastSeen;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${livefeed.rate:2000}", scheduler = "scheduler")
    public void sendLiveNow() {
        try {
            LiveNowSnapshot snapshot = statusService.getLiveNowSnapshot();
            String payload = objectMapper.writeValueAsString(snapshot);
            topicPublisher.publish("/topic/live_now", payload);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize LiveNowSnapshot", e);
        } catch (Exception e) {
            log.warn("sendLiveNow failed", e);
        }
    }

    @Scheduled(fixedDelay = 10000, scheduler = "scheduler")
    public void logLaggingDevices() {
        Instant now = Instant.now();
        lastSeen.forEach((id, ts) -> {
            if (Duration.between(ts, now).toSeconds() > 60) {
                log.debug("Device {} no message for >60s (lastSeen={})", id, ts);
            }
        });
    }
}
