package se.hydroleaf.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import se.hydroleaf.controller.dto.LedCommandRequest;
import se.hydroleaf.controller.dto.LedCommandResponse;
import se.hydroleaf.mqtt.MqttService;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@ConditionalOnProperty(prefix = "mqtt", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ActuatorCommandService {

    private static final String LED_COMMAND_TOPIC = "actuator/led/cmd";
    private static final String DEFAULT_SYSTEM = "S01";
    private static final String DEFAULT_DEVICE_ID = "R01";

    private final ObjectMapper objectMapper;
    private final MqttService mqttService;

    public ActuatorCommandService(ObjectMapper objectMapper, MqttService mqttService) {
        this.objectMapper = objectMapper;
        this.mqttService = mqttService;
    }

    public LedCommandResponse publishLedCommand(LedCommandRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("system", normalizeSystem(request.system()));
        payload.put("layer", normalizeLayer(request.layer()));
        payload.put("deviceId", normalizeDeviceId(request.deviceId()));
        payload.put("controller", normalizeController(request.controller()));
        payload.put("command", normalizeCommand(request.command()));
        if (request.durationSec() != null) {
            payload.put("durationSec", request.durationSec());
        }

        try {
            String json = objectMapper.writeValueAsString(payload);
            mqttService.publish(LED_COMMAND_TOPIC, json);
            return new LedCommandResponse(LED_COMMAND_TOPIC, json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize LED command", e);
        }
    }

    private String normalizeSystem(String system) {
        if (system == null || system.isBlank()) {
            return DEFAULT_SYSTEM;
        }
        return system.trim().toUpperCase();
    }

    private String normalizeLayer(String layer) {
        if (layer == null) {
            return null;
        }

        String trimmed = layer.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        String upper = trimmed.toUpperCase();
        if (upper.matches("^L?0?1$")) {
            return "L01";
        }
        if (upper.matches("^L?0?2$")) {
            return "L02";
        }
        return upper;
    }

    private String normalizeDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return DEFAULT_DEVICE_ID;
        }
        return deviceId.trim();
    }

    private String normalizeController(String controller) {
        if (controller == null || controller.isBlank()) {
            return null;
        }
        return controller.trim().toLowerCase();
    }

    private String normalizeCommand(String command) {
        if (command == null || command.isBlank()) {
            return null;
        }
        return command.trim().toUpperCase();
    }
}
