package se.hydroleaf.mqtt;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import se.hydroleaf.model.TopicName;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * MQTT bridge:
 * - Connects to broker and subscribes to configured topics.
 * - Delegates message parsing and persistence to {@link MqttMessageHandler}.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "mqtt", name = "enabled", havingValue = "true", matchIfMissing = false)
public class MqttService implements MqttCallbackExtended {

    @Value("${mqtt.brokerUri}")
    private String brokerUri;

    @Value("${mqtt.clientId:hydroleaf-backend}")
    private String clientId;

    @Value("${mqtt.qos:1}")
    private int qos;

    @Value("${mqtt.topics:growSensors/#,waterTank/#,germinationTopic/#,actuator/oxygenPump/#,water_flow}")
    private String[] topics;

    private MqttClient client;
    private MqttConnectOptions connectOptions;

    private final MqttMessageHandler messageHandler;

    public MqttService(MqttMessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @PostConstruct
    public void start() {
        MemoryPersistence persistence = new MemoryPersistence();
        connectOptions = new MqttConnectOptions();
        connectOptions.setAutomaticReconnect(true);
        connectOptions.setCleanSession(true);
        connectOptions.setConnectionTimeout(10);

        try {
            client = new MqttClient(brokerUri, clientId, persistence);
            client.setCallback(this);

            log.info("MQTT connecting to {} with clientId={} topics={}", brokerUri, clientId, Arrays.toString(topics));
            client.connect(connectOptions);
        } catch (MqttException e) {
            log.error("MQTT initial connection failed; will rely on reconnect/publish attempts", e);
        }
    }

    @PreDestroy
    public void stop() {
        try {
            if (client != null && client.isConnected()) client.disconnect();
            if (client != null) client.close();
        } catch (Exception e) {
            log.warn("MQTT disconnect/close failed", e);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("MQTT connection lost: {}", cause.toString());
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        log.info("MQTT connection complete (reconnect={})", reconnect);
        for (String t : topics) {
            String topic = t.trim();
            if (!topic.isEmpty()) {
                try {
                    client.subscribe(topic, qos);
                    log.info("MQTT subscribed: {} (qos={})", topic, qos);
                } catch (MqttException e) {
                    log.warn("MQTT subscribe failed for {}", topic, e);
                }
            }
        }
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

    public void publish(String topic, String payload) {
        if (client == null) {
            throw new IllegalStateException("MQTT client not initialized");
        }

        try {
            if (!client.isConnected()) {
                client.connect(connectOptions);
            }

            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(qos);
            client.publish(topic, message);
        } catch (MqttException e) {
            throw new IllegalStateException("Failed to publish MQTT message to " + topic, e);
        }
    }

}
