package se.hydroleaf.service;

import org.springframework.stereotype.Service;
import se.hydroleaf.model.LatestSensorValue;
import se.hydroleaf.model.TopicName;
import se.hydroleaf.repository.LatestSensorValueRepository;
import se.hydroleaf.repository.dto.TopicSensorsResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class TopicService {

    private final LatestSensorValueRepository latestSensorValueRepository;

    public TopicService(LatestSensorValueRepository latestSensorValueRepository) {
        this.latestSensorValueRepository = latestSensorValueRepository;
    }

    public TopicSensorsResponse getSensorTypesByTopic() {
        Map<TopicName, Set<String>> sensorsByTopic = latestSensorValueRepository.findAll().stream()
                .filter(lsv -> lsv.getDevice() != null && lsv.getDevice().getTopic() != null)
                .filter(lsv -> lsv.getSensorType() != null && !lsv.getSensorType().isBlank())
                .collect(Collectors.groupingBy(lsv -> lsv.getDevice().getTopic(),
                        () -> new EnumMap<>(TopicName.class),
                        Collectors.mapping(LatestSensorValue::getSensorType,
                                Collectors.toCollection(TreeSet::new))));

        List<TopicSensorsResponse.TopicSensors> topics = Arrays.stream(TopicName.values())
                .map(topicName -> {
                    Set<String> sensorTypes = sensorsByTopic.getOrDefault(topicName, Collections.emptySet());
                    return new TopicSensorsResponse.TopicSensors(topicName.name(), new ArrayList<>(sensorTypes));
                })
                .toList();

        return new TopicSensorsResponse(Instant.now().toString(), topics);
    }
}
