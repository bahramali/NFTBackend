package se.hydroleaf.mqtt;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

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
public class MqttService implements MqttCallback {

    @Value("${mqtt.brokerUri}")
    private String brokerUri;

    @Value("${mqtt.clientId:hydroleaf-backend}")
    private String clientId;

    @Value("${mqtt.qos:1}")
    private int qos;

    @Value("${mqtt.topics:growSensors/#,waterTank/#,actuator/oxygenPump/#}")
    private String[] topics;

    private MqttClient client;

    private final MqttMessageHandler messageHandler;

    public MqttService(MqttMessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @PostConstruct
    public void start() throws Exception {
        MemoryPersistence persistence = new MemoryPersistence();
        client = new MqttClient(brokerUri, clientId, persistence);
        client.setCallback(this);

        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setAutomaticReconnect(true);
        opts.setCleanSession(true);
        opts.setConnectionTimeout(10);

        log.info("MQTT connecting to {} with clientId={} topics={}", brokerUri, clientId, Arrays.toString(topics));
        client.connect(opts);

        for (String t : topics) {
            String topic = t.trim();
            if (!topic.isEmpty()) {
                client.subscribe(topic, qos);
                log.info("MQTT subscribed: {} (qos={})", topic, qos);
            }
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
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        messageHandler.handle(topic, payload);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // not publishing; ignore
    }

}
