package se.hydroleaf.repository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import se.hydroleaf.model.DeviceType;

@Repository
public class SensorDataRepositoryImpl implements SensorDataRepositoryCustom {

    private final AggregateRepository aggregateRepository;

    public SensorDataRepositoryImpl(AggregateRepository aggregateRepository) {
        this.aggregateRepository = aggregateRepository;
    }

    @Override
    public AverageCount getLatestAverage(String system, String layer, DeviceType sensorType) {
        return aggregateRepository.getLatestAverage(system, layer, sensorType.getName(), "sensor_data");
    }

    @Override
    public Map<DeviceType, AverageCount> getLatestAverages(String system, String layer, List<DeviceType> sensorTypes) {
        Map<String, AverageCount> result = aggregateRepository.getLatestAverages(
                system,
                layer,
                sensorTypes.stream().map(DeviceType::getName).toList(),
                "sensor_data");
        return result.entrySet().stream()
                .collect(Collectors.toMap(e -> DeviceType.fromName(e.getKey()), Map.Entry::getValue));
    }
}

