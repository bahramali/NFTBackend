package se.hydroleaf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import se.hydroleaf.model.SensorRecord;
import se.hydroleaf.repository.SensorRecordRepository;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public Optional<SensorRecord> getLatestRecord() {
        return repository.findTopByOrderByTimestampDesc();
    }

    public List<SensorRecord> getRecordsBetween(Instant from, Instant to) {
        return repository.findByTimestampBetween(from, to);
    }

    public Map<String, Double> calculateStatistics(String type, Duration range) {
        Instant to = Instant.now();
        Instant from = to.minus(range);
        List<SensorRecord> records = repository.findByTimestampBetween(from, to);
        List<Double> values = new ArrayList<>();
        for (SensorRecord r : records) {
            try {
                JsonNode node = objectMapper.readTree(r.getSensors());
                if (node.has(type)) {
                    values.add(node.get(type).asDouble());
                }
            } catch (Exception ignored) {
            }
        }
        double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(Double.NaN);
        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(Double.NaN);
        double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
        Map<String, Double> stats = new HashMap<>();
        stats.put("min", min);
        stats.put("max", max);
        stats.put("avg", avg);
        return stats;
    }
}
