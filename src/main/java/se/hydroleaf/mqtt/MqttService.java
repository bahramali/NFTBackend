package se.hydroleaf.mqtt;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import se.hydroleaf.service.StatusService;

import java.nio.charset.StandardCharsets;

/**
 * MQTT bridge:
 * - Connects to broker and subscribes to configured topics.
 * - Delegates message parsing and persistence to {@link MqttMessageHandler}.
 * - Periodically pushes live snapshot to /topic/live_now via STOMP.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "mqtt", name = "enabled", havingValue = "true", matchIfMissing = false)
public class MqttService implements MqttCallback {

    private final SimpMessagingTemplate messagingTemplate;
    private final StatusService statusService;
    private final MqttClientManager clientManager;
    private final MqttMessageHandler messageHandler;

    public MqttService(SimpMessagingTemplate messagingTemplate,
                       StatusService statusService,
                       MqttClientManager clientManager,
                       MqttMessageHandler messageHandler) {
        this.messagingTemplate = messagingTemplate;
        this.statusService = statusService;
        this.clientManager = clientManager;
        this.messageHandler = messageHandler;
    }

    @PostConstruct
    public void start() throws Exception {
        clientManager.start(this);
    }

    @PreDestroy
    public void stop() {
        clientManager.stop();
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("MQTT connection lost: {}", cause.toString());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        messageHandler.handle(topic, payload);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // not publishing; ignore
    }

    // -------- live feed --------

    @Scheduled(fixedRate = 2000)
    public void sendLiveNow() {
        try {
            Object snapshot = statusService.getAllSystemLayerAverages();
            messagingTemplate.convertAndSend("/topic/live_now", snapshot);
        } catch (Exception e) {
            // avoid killing the scheduler on transient NPEs
            log.warn("sendLiveNow failed: {}", e.getMessage());
        }
    }
}
