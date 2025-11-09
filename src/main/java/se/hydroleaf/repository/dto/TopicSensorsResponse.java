package se.hydroleaf.repository.dto;

import java.util.List;

public record TopicSensorsResponse(
        String version,
        List<TopicSensors> topics
) {
    public record TopicSensors(String topic, List<String> sensorTypes) {}
}
