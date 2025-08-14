package se.hydroleaf.mqtt;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * MQTT bridge:
 * - Connects to broker and subscribes to configured topics.
 * - Delegates message parsing and persistence to {@link MqttMessageHandler}.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "mqtt", name = "enabled", havingValue = "true", matchIfMissing = false)
public class MqttService implements MqttCallback {

    private final MqttClientManager clientManager;
    private final MqttMessageHandler messageHandler;

    public MqttService(MqttClientManager clientManager,
                       MqttMessageHandler messageHandler) {
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

}
