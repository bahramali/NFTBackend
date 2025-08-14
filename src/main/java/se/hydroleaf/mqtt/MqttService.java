package se.hydroleaf.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import se.hydroleaf.model.Device;
import se.hydroleaf.model.DeviceGroup;
import se.hydroleaf.repository.DeviceGroupRepository;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.service.RecordService;
import se.hydroleaf.service.StatusService;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MQTT bridge:
 * - Connects to broker and subscribes to configured topics.
 * - Parses incoming JSON, derives composite_id (payload or topic),
 * auto-provisions Device/DeviceGroup if missing, and persists data.
 * - Periodically pushes live snapshot to /topic/live_now via STOMP.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "mqtt", name = "enabled", havingValue = "true", matchIfMissing = false)
public class MqttService implements MqttCallback {

    private final ObjectMapper objectMapper;
    private final RecordService recordService;
    private final SimpMessagingTemplate messagingTemplate;
    private final StatusService statusService;
    private final DeviceRepository deviceRepo;
    private final DeviceGroupRepository groupRepo;

    @Value("${mqtt.brokerUri}")
    private String brokerUri;

    @Value("${mqtt.clientId:hydroleaf-backend}")
    private String clientId;

    @Value("${mqtt.qos:1}")
    private int qos;

    // e.g. growSensors/#, waterTank/#, actuator/oxygenPump/#
    @Value("${mqtt.topics:growSensors/#,waterTank/#,actuator/oxygenPump/#}")
    private String[] topics;

    // optional, used when auto-provisioning groups
    @Value("${mqtt.topicPrefix:}")
    private String topicPrefix;

    private MqttClient client;
    private final ConcurrentHashMap<String, Instant> lastSeen = new ConcurrentHashMap<>();

    public MqttService(ObjectMapper objectMapper,
                       RecordService recordService,
                       SimpMessagingTemplate messagingTemplate,
                       StatusService statusService,
                       DeviceRepository deviceRepo,
                       DeviceGroupRepository groupRepo) {
        this.objectMapper = objectMapper;
        this.recordService = recordService;
        this.messagingTemplate = messagingTemplate;
        this.statusService = statusService;
        this.deviceRepo = deviceRepo;
        this.groupRepo = groupRepo;
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
        messagingTemplate.convertAndSend("/topic/" + topic, payload);
        try {
            JsonNode node = objectMapper.readTree(payload);

            // 1) composite_id from payload or derived from topic
            String compositeId = readCompositeId(node);
            if (compositeId == null) {
                compositeId = deriveCompositeIdFromTopic(topic);
            }
            if (compositeId == null || compositeId.isBlank()) {
                log.warn("MQTT parse/handle failed on topic {}: missing composite_id", topic);
                return;
            }

            // 2) auto-provision device/group if missing
            ensureDevice(compositeId, topic);

            // 3) forward all payloads to RecordService
            recordService.saveRecord(compositeId, node);

            lastSeen.put(compositeId, Instant.now());
        } catch (Exception ex) {
            log.error("MQTT handle error for topic {}: {}", topic, ex.getMessage(), ex);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // not publishing; ignore
    }

    // -------- util: composite id --------

    private static String readCompositeId(JsonNode n) {
        if (n == null) return null;
        if (n.hasNonNull("composite_id")) return n.get("composite_id").asText();
        if (n.hasNonNull("compositeId")) return n.get("compositeId").asText();
        return null;
    }

    // Accepts topics like: growSensors/S01/L02/esp32-01  -> S01-L02-esp32-01
    private static final Pattern GROW_PATTERN =
            Pattern.compile("^growSensors/(S\\d+)/(L\\d+)/([^/]+)$");

    private String deriveCompositeIdFromTopic(String topic) {
        if (topic == null) return null;
        Matcher m = GROW_PATTERN.matcher(topic);
        if (m.find()) {
            return m.group(1) + "-" + m.group(2) + "-" + m.group(3);
        }
        return null;
    }

    // -------- util: ensure device/group --------

    private Device ensureDevice(String compositeId, String topic) {
        return deviceRepo.findById(compositeId).orElseGet(() -> {
            Device d = new Device();
            d.setCompositeId(compositeId);

            String[] parts = compositeId.split("-");
            if (parts.length >= 3) {
                d.setSystem(parts[0]);
                d.setLayer(parts[1]);
                d.setDeviceId(parts[2]);
            }

            // choose a group by exact topic (best) or fallback to a default one
            String grpKey = (topic != null && !topic.isBlank()) ? topic.split("/")[0] : topicPrefix;
            DeviceGroup g = findOrCreateGroup(grpKey == null ? "default" : grpKey);
            d.setGroup(g);

            return deviceRepo.save(d);
        });
    }

    private DeviceGroup findOrCreateGroup(String mqttKey) {
        Optional<DeviceGroup> g = groupRepo.findByMqttTopic(mqttKey);
        if (g.isPresent()) return g.get();

        DeviceGroup ng = new DeviceGroup();
        ng.setMqttTopic(mqttKey);
        return groupRepo.save(ng);
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

    // Optional: log devices that stopped sending data
    @Scheduled(fixedDelay = 10000)
    public void logLaggingDevices() {
        Instant now = Instant.now();
        lastSeen.forEach((id, ts) -> {
            if (Duration.between(ts, now).toSeconds() > 60) {
                log.debug("Device {} no message for >60s (lastSeen={})", id, ts);
            }
        });
    }
}
