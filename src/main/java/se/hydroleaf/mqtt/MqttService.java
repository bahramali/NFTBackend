package se.hydroleaf.mqtt;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import se.hydroleaf.dto.StatusAllAverageResponse;
import se.hydroleaf.service.ActuatorService;
import se.hydroleaf.service.RecordService;
import se.hydroleaf.service.StatusService;

@Slf4j
@Service
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true", matchIfMissing = true)
public class MqttService implements MqttCallback {

    private final RecordService recordService;
    private final ActuatorService actuatorService;
    private final SimpMessagingTemplate messagingTemplate;
    private final StatusService statusService;

    private final ConcurrentHashMap<String, Instant> lastSaveTimestamps = new ConcurrentHashMap<>();

    @Value("${mqtt.broker}")
    private String broker;

    private IMqttClient client;

    public MqttService(RecordService recordService, ActuatorService actuatorService,
                       SimpMessagingTemplate messagingTemplate, StatusService statusService) {
        this.recordService = recordService;
        this.actuatorService = actuatorService;
        this.messagingTemplate = messagingTemplate;
        this.statusService = statusService;
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
            client.subscribe("actuator/oxygenPum");
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
        log.info("Topic is {}", topic);
        Instant now = Instant.now();
        Instant lastSaved = lastSaveTimestamps.get(topic);
        boolean shouldPersist = lastSaved == null || Duration.between(lastSaved, now).getSeconds() >= 5;
        if (shouldPersist) {
            if ("actuator/oxygenPum".equals(topic)) {
                try {
                    actuatorService.saveOxygenPumpStatus(payload);
                } catch (Exception e) {
                    log.error("Failed to store MQTT actuator message for topic {}", topic, e);
                }
            } else {
                try {
                    recordService.saveMessage(topic, payload);
                } catch (Exception e) {
                    log.error("Failed to store MQTT message for topic {}", topic, e);
                }
            }
            lastSaveTimestamps.put(topic, now);
        }
        messagingTemplate.convertAndSend("/topic/" + topic, payload);
        StatusAllAverageResponse allAverages = statusService.getAllAverages("S01", "L01");
        log.info("statusService.getAllAverages(\"S01\", \"L01\")= {}", allAverages);
        messagingTemplate.convertAndSend("/topic/live_now", allAverages);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // not used since we only subscribe
    }
}
