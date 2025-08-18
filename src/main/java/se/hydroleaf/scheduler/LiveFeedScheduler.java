package se.hydroleaf.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import se.hydroleaf.dto.snapshot.LiveNowSnapshot;
import se.hydroleaf.mqtt.TopicPublisher;
import se.hydroleaf.service.StatusService;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Periodic tasks related to live device feed.
 */
@Slf4j
@Service
public class LiveFeedScheduler {

    private final StatusService statusService;
    private final TopicPublisher topicPublisher;
    private final ObjectMapper objectMapper;

    public LiveFeedScheduler(StatusService statusService,
                             TopicPublisher topicPublisher,
                             ObjectMapper objectMapper) {
        this.statusService = statusService;
        this.topicPublisher = topicPublisher;
        this.objectMapper = objectMapper;
    }

    private final AtomicBoolean sending = new AtomicBoolean(false);

    // Using fixedDelay ensures that if executions are delayed for any reason,
    // multiple invocations do not queue up and publish in rapid succession.
    @Scheduled(fixedDelayString = "${livefeed.rate:2000}", scheduler = "scheduler")
    public void sendLiveNow() {
        if (!sending.compareAndSet(false, true)) {
            log.debug("sendLiveNow already running; skipping");
            return;
        }
        try {
            LiveNowSnapshot snapshot = statusService.getLiveNowSnapshot();
            String payload = objectMapper.writeValueAsString(snapshot);
            topicPublisher.publish("/topic/live_now", payload);
            log.debug("live_now message is: {}", payload);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize LiveNowSnapshot", e);
        } catch (Exception e) {
            log.warn("sendLiveNow failed", e);
        } finally {
            sending.set(false);
        }
    }

}
