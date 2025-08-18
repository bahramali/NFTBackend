package se.hydroleaf.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes messages to STOMP topics when publishing is enabled.
 * If disabled, it logs the intended destination and payload instead.
 */
@Slf4j
@Service
public class TopicPublisher {

    private final boolean publishEnabled;
    private final SimpMessagingTemplate messagingTemplate;

    public TopicPublisher(@Value("${mqtt.publishEnabled:true}") boolean publishEnabled,
                          SimpMessagingTemplate messagingTemplate) {
        this.publishEnabled = publishEnabled;
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(String destination, String payload) {
        if (publishEnabled) {
            if (destination.contains("live_now")) {
                log.debug("Publishing to {} : ", destination);
            }
            messagingTemplate.convertAndSend(destination, payload);
        } else {
            if ("/topic/live_now".equalsIgnoreCase(destination)) {
                log.debug("Publishing disabled. Should publish to {} with payload: {}", destination, payload);
            }
        }
    }
}

