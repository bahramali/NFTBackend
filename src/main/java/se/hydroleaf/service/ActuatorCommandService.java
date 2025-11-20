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

    private final ObjectMapper objectMapper;
    private final MqttService mqttService;

    public ActuatorCommandService(ObjectMapper objectMapper, MqttService mqttService) {
        this.objectMapper = objectMapper;
        this.mqttService = mqttService;
    }

    public LedCommandResponse publishLedCommand(LedCommandRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("system", request.system());
        payload.put("layer", request.layer());
        payload.put("deviceId", request.deviceId());
        payload.put("controller", request.controller());
        payload.put("command", request.command());
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
}
