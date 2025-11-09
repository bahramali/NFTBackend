package se.hydroleaf.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.hydroleaf.model.Device;
import se.hydroleaf.model.LatestSensorValue;
import se.hydroleaf.model.TopicName;
import se.hydroleaf.repository.LatestSensorValueRepository;
import se.hydroleaf.repository.dto.TopicSensorsResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopicServiceTest {

    @Mock
    LatestSensorValueRepository latestSensorValueRepository;

    private TopicService topicService;

    @BeforeEach
    void setup() {
        topicService = new TopicService(latestSensorValueRepository);
    }

    @Test
    void getSensorTypesByTopicAggregatesAndSortsSensors() {
        Device growDevice = Device.builder().compositeId("GROW-1").topic(TopicName.growSensors).build();
        Device waterDevice = Device.builder().compositeId("WATER-1").topic(TopicName.waterTank).build();
        LatestSensorValue ph = LatestSensorValue.builder().device(growDevice).sensorType("ph").build();
        LatestSensorValue temp = LatestSensorValue.builder().device(growDevice).sensorType("temperature").build();
        LatestSensorValue duplicatePh = LatestSensorValue.builder().device(growDevice).sensorType("ph").build();
        LatestSensorValue level = LatestSensorValue.builder().device(waterDevice).sensorType("level").build();
        LatestSensorValue missingType = LatestSensorValue.builder().device(waterDevice).build();

        when(latestSensorValueRepository.findAll())
                .thenReturn(List.of(ph, temp, duplicatePh, level, missingType));

        TopicSensorsResponse response = topicService.getSensorTypesByTopic();

        assertNotNull(response.version());
        assertEquals(List.of("ph", "temperature"), sensorsForTopic(response, TopicName.growSensors));
        assertEquals(List.of("level"), sensorsForTopic(response, TopicName.waterTank));
        assertEquals(List.of(), sensorsForTopic(response, TopicName.germinationTopic));
    }

    private List<String> sensorsForTopic(TopicSensorsResponse response, TopicName topicName) {
        return response.topics().stream()
                .filter(t -> t.topic().equals(topicName.name()))
                .findFirst()
                .map(TopicSensorsResponse.TopicSensors::sensorTypes)
                .orElse(List.of());
    }
}
