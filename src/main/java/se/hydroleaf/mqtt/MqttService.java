package se.hydroleaf.mqtt;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import se.hydroleaf.service.RecordService;

@Slf4j
@Service
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true", matchIfMissing = true)
public class MqttService implements MqttCallback {

    private final RecordService recordService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${mqtt.broker}")
    private String broker;

    private IMqttClient client;

    public MqttService(RecordService recordService, SimpMessagingTemplate messagingTemplate) {
        this.recordService = recordService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostConstruct
    public void start() {
        try {
            String clientId = MqttClient.generateClientId();
            client = new MqttClient(broker, clientId);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            client.setCallback(this);
            client.connect(options);

            client.subscribe("growSensors");
            client.subscribe("rootImages");
            client.subscribe("waterOutput");
            client.subscribe("waterTank");
            log.info("Connected to MQTT broker {}", broker);
        } catch (MqttException e) {
            log.warn("Failed to connect to MQTT broker {}", broker, e);
        }
    }

    @PreDestroy
    public void stop() {
        if (client != null && client.isConnected()) {
            try {
                client.disconnect();
            } catch (MqttException e) {
                log.warn("Error disconnecting MQTT client", e);
            }
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("MQTT connection lost", cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        log.info("1- Received MQTT message from topic: {}: with payload: {}", topic, payload);
        recordService.saveMessage(topic, payload);
        messagingTemplate.convertAndSend("/topic/" + topic, payload);
        log.info("3- payload is sent");
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // not used since we only subscribe
    }
}
