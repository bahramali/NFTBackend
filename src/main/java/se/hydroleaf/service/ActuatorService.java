package se.hydroleaf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.hydroleaf.model.OxygenPumpStatus;
import se.hydroleaf.repository.OxygenPumpStatusRepository;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.util.InstantUtil;

@Service
public class ActuatorService {

    private final OxygenPumpStatusRepository oxygenPumpStatusRepository;
    private final DeviceRepository deviceRepository;
    private final ObjectMapper objectMapper;

    public ActuatorService(OxygenPumpStatusRepository oxygenPumpStatusRepository,
                           DeviceRepository deviceRepository,
                           ObjectMapper objectMapper) {
        this.oxygenPumpStatusRepository = oxygenPumpStatusRepository;
        this.deviceRepository = deviceRepository;
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
            if (!node.path("compositeId").isMissingNode()) {
                String compositeId = node.path("compositeId").asText();
                deviceRepository.findById(compositeId)
                        .ifPresent(status::setDevice);
            }
            oxygenPumpStatusRepository.save(status);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse and save oxygen pump status", e);
        }
    }
}
