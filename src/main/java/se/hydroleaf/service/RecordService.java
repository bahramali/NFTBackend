package se.hydroleaf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import se.hydroleaf.model.SensorRecord;
import se.hydroleaf.repository.SensorRecordRepository;

import java.io.IOException;
import java.time.Instant;

@Service
public class RecordService {

    private final SensorRecordRepository repository;
    private final ObjectMapper objectMapper;

    public RecordService(SensorRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void saveMessage(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            SensorRecord record = new SensorRecord();
            record.setDeviceId(node.path("deviceId").asText());
            record.setTimestamp(Instant.parse(node.path("timestamp").asText()));
            record.setLocation(node.path("location").asText());
            record.setSensors(node.path("sensors").toString());
            record.setHealth(node.path("health").toString());
            repository.save(record);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse message", e);
        }
    }
}
