package se.hydroleaf.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Publishes messages to STOMP topics when publishing is enabled.
 */
@Service
@Slf4j
public class TopicPublisher {

    private final boolean publishEnabled;
    private final SimpMessagingTemplate messagingTemplate;
    private final Set<String> loggedDestinations = ConcurrentHashMap.newKeySet();

    public TopicPublisher(@Value("${mqtt.publishEnabled:true}") boolean publishEnabled,
                          SimpMessagingTemplate messagingTemplate) {
        this.publishEnabled = publishEnabled;
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(String destination, String payload) {
        publish(destination, (Object) payload, null, null);
    }

    public void publish(String destination, Object payload, String compositeId, String kind) {
        if (publishEnabled) {
            logDestinationOnce(destination);
            messagingTemplate.convertAndSend(destination, payload);
        }
    }

    private void logDestinationOnce(String destination) {
        if (destination == null || destination.isBlank()) {
            return;
        }
        if (loggedDestinations.add(destination)) {
            log.info("WS topic publishing enabled for dest={}", destination);
        }
    }
}
