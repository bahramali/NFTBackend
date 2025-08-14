package se.hydroleaf.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Manages the lifecycle of an MQTT client.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "mqtt", name = "enabled", havingValue = "true", matchIfMissing = false)
public class MqttClientManager {

    @Value("${mqtt.brokerUri}")
    private String brokerUri;

    @Value("${mqtt.clientId:hydroleaf-backend}")
    private String clientId;

    @Value("${mqtt.qos:1}")
    private int qos;

    @Value("${mqtt.topics:growSensors/#,waterTank/#,actuator/oxygenPump/#}")
    private String[] topics;

    private MqttClient client;

    public void start(MqttCallback callback) throws MqttException {
        MemoryPersistence persistence = new MemoryPersistence();
        client = new MqttClient(brokerUri, clientId, persistence);
        client.setCallback(callback);

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

    public void stop() {
        try {
            if (client != null && client.isConnected()) client.disconnect();
            if (client != null) client.close();
        } catch (Exception e) {
            log.warn("MQTT disconnect/close failed", e);
        }
    }
}
