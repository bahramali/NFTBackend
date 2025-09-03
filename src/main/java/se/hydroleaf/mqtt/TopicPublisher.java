package se.hydroleaf.mqtt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes messages to STOMP topics when publishing is enabled.
 */
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
            messagingTemplate.convertAndSend(destination, payload);
        }
    }
}

