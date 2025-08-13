package se.hydroleaf.mqtt;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import se.hydroleaf.service.RecordService;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MQTT -> persist sensor records, then push live aggregates over STOMP.
 * - Device PK is composite_id, so we must resolve it per message (payload or topic).
 * - Uses Paho client with auto-reconnect.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "mqtt", name = "enabled", havingValue = "true", matchIfMissing = false)
public class MqttService implements MqttCallback {

    // ====== Config ======
    @Value("${mqtt.brokerUri}")
    private String brokerUri;

    @Value("${mqtt.clientId:hydroleaf-backend}")
    private String clientId;

    /** Comma-separated topic filters, e.g. "sensors/#,devices/+/+/+" */
    @Value("${mqtt.topics:sensors/#}")
    private String topics;

    /** MQTT QoS for subscriptions (0,1,2). */
    @Value("${mqtt.qos:1}")
    private int qos;

    /** Optional string prefix for topics, ignored if blank. */
    @Value("${mqtt.topicPrefix:}")
    private String topicPrefix;

    // ====== Deps ======
    private final ObjectMapper objectMapper;
    private final RecordService recordService;
    private final SimpMessagingTemplate messagingTemplate;
    private final StatusService statusService; // <-- replace type with your real aggregation service bean

    // ====== Runtime ======
    private MqttClient client;
    private final ConcurrentHashMap<String, Instant> lastSeen = new ConcurrentHashMap<>();
    private volatile boolean shuttingDown = false;

    public MqttService(ObjectMapper objectMapper,
                       RecordService recordService,
                       SimpMessagingTemplate messagingTemplate,
                       StatusService statusService) {
        this.objectMapper = objectMapper;
        this.recordService = recordService;
        this.messagingTemplate = messagingTemplate;
        this.statusService = statusService;
    }

    // ---------- Lifecycle ----------
    @PostConstruct
    public void start() throws Exception {
        Objects.requireNonNull(brokerUri, "mqtt.brokerUri is required");

        client = new MqttClient(brokerUri, clientId, new MemoryPersistence());

        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setAutomaticReconnect(true);
        opts.setCleanSession(true);
        opts.setConnectionTimeout(10);
        opts.setKeepAliveInterval(20);

            client.setCallback(this);
        client.connect(opts);

        for (String t : splitTopics(topics)) {
            String topic = normalizeTopic(t);
            client.subscribe(topic, qos);
            log.info("MQTT subscribed to topic: {}", topic);
        }

        log.info("MQTT connected: {} as {}", brokerUri, clientId);
    }

    @PreDestroy
    public void stop() {
        shuttingDown = true;
        try {
        if (client != null && client.isConnected()) {
                client.disconnectForcibly(1000, 1000);
            }
        } catch (Exception e) {
            log.warn("MQTT disconnect error: {}", e.getMessage());
        } finally {
            try { if (client != null) client.close(); } catch (Exception ignore) {}
        }
    }

    // ---------- MqttCallback ----------
    @Override
    public void connectionLost(Throwable cause) {
        if (shuttingDown) return;
        log.warn("MQTT connection lost: {}", cause == null ? "unknown" : cause.getMessage());
        // Paho will auto-reconnect because we enabled setAutomaticReconnect(true)
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        final String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                try {
            JsonNode json = objectMapper.readTree(payload);

            // 1) Try to get composite_id from payload
            String compositeId = findCompositeIdInPayload(json);

            // 2) If not present, try to derive from topic segments
            if (compositeId == null) {
                compositeId = deriveCompositeIdFromTopic(topic);
                }

            if (compositeId == null) {
                log.debug("Skip message (no composite_id) topic={} payload={}", topic, shrink(payload));
                return;
            }

            // Persist record (delegates JSON parsing of values/health/pump to RecordService)
            recordService.saveRecord(compositeId, json);

            lastSeen.put(compositeId, Instant.now());
        } catch (Exception ex) {
            // Avoid flooding logs: just print short message + sample
            log.warn("MQTT parse/handle failed on topic {}: {}", topic, ex.getMessage());
            log.debug("Payload: {}", shrink(payload));
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // not publishing; ignore
    }

    // ---------- Live push ----------
    @Scheduled(fixedRate = 2000)
    public void sendLiveNow() {
        // getAllSystemLayerAverages() should return the exact object you push to /topic/live_now
        messagingTemplate.convertAndSend("/topic/live_now", statusService.getAllSystemLayerAverages());
    }

    // ---------- Helpers ----------
    private static String[] splitTopics(String csv) {
        return csv == null ? new String[0] :
                csv.replace(" ", "").split("\\s*,\\s*");
    }

    private String normalizeTopic(String t) {
        if (t == null || t.isBlank()) return t;
        if (topicPrefix == null || topicPrefix.isBlank()) return t;
        // Ensure no duplicate slashes
        String prefix = topicPrefix.endsWith("/") ? topicPrefix.substring(0, topicPrefix.length() - 1) : topicPrefix;
        String tail = t.startsWith("/") ? t.substring(1) : t;
        return prefix + "/" + tail;
    }

    /** Try to read composite_id / compositeId from JSON payload. */
    private String findCompositeIdInPayload(JsonNode json) {
        if (json == null) return null;
        if (json.hasNonNull("composite_id")) return json.get("composite_id").asText();
        if (json.hasNonNull("compositeId"))  return json.get("compositeId").asText();
        return null;
    }

    /**
     * Try to derive Sxx-Lxx-device from topic parts.
     * Examples this method understands:
     *  - sensors/S01/L02/esp32-01
     *  - hydroleaf/S02/L01/dev123/values
     *  - S03/L04/gw-7
     */
    private String deriveCompositeIdFromTopic(String topic) {
        if (topic == null) return null;
        String[] parts = topic.split("/");
        String s = null, l = null, dev = null;
        for (String p : parts) {
            if (s == null && p.matches("S\\d+")) { s = p; continue; }
            if (l == null && p.matches("L\\d+")) { l = p; continue; }
            // pick the first non-empty, non wild-card token as device id after we saw S and L
            if (s != null && l != null && dev == null && !p.isBlank() && !p.equals("#") && !p.equals("+")) {
                dev = p;
                break;
            }
        }
        if (s != null && l != null && dev != null) return s + "-" + l + "-" + dev;
        return null;
        }

    private static String shrink(String s) {
        if (s == null) return null;
        return s.length() <= 512 ? s : (s.substring(0, 512) + " ...");
    }

    // ---------- Optional: health check ----------
    @Scheduled(fixedDelay = 10000)
    public void logLaggingDevices() {
        Instant now = Instant.now();
        lastSeen.forEach((id, ts) -> {
            if (Duration.between(ts, now).toSeconds() > 60) {
                log.debug("Device {} no message for >60s (lastSeen={})", id, ts);
            }
        });
    }

    // ====== Replace with your real aggregation/live facade ======
    public interface StatusService {
        Object getAllSystemLayerAverages();
    }
}
