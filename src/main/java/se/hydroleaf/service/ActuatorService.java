package se.hydroleaf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.hydroleaf.model.OxygenPumpStatus;
import se.hydroleaf.repository.OxygenPumpStatusRepository;
import se.hydroleaf.util.InstantUtil;

@Service
public class ActuatorService {

    private final OxygenPumpStatusRepository oxygenPumpStatusRepository;
    private final ObjectMapper objectMapper;

    public ActuatorService(OxygenPumpStatusRepository oxygenPumpStatusRepository, ObjectMapper objectMapper) {
        this.oxygenPumpStatusRepository = oxygenPumpStatusRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void saveOxygenPumpStatus(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            OxygenPumpStatus status = oxygenPumpStatusRepository.findTopByOrderByIdAsc()
                    .orElseGet(OxygenPumpStatus::new);
            status.setTimestamp(InstantUtil.parse(node.path("timestamp").asText()));

            String statusString = node.path("status").asText();
            boolean parsedStatus = "on".equalsIgnoreCase(statusString);
            status.setStatus(parsedStatus);
            if (!node.path("system").isMissingNode()) {
                status.setSystem(node.path("system").asText());
            }
            if (!node.path("layer").isMissingNode()) {
                status.setLayer(node.path("layer").asText());
            }
            if (!node.path("deviceId").isMissingNode()) {
                status.setDeviceId(node.path("deviceId").asText());
            }
            if (!node.path("compositeId").isMissingNode()) {
                status.setCompositeId(node.path("compositeId").asText());
            }
            oxygenPumpStatusRepository.save(status);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse and save oxygen pump status", e);
        }
    }
}
