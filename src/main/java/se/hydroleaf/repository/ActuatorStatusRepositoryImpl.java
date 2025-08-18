package se.hydroleaf.repository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import se.hydroleaf.model.DeviceType;

@Repository
public class ActuatorStatusRepositoryImpl implements ActuatorStatusRepositoryCustom {

    private final AggregateRepository aggregateRepository;

    public ActuatorStatusRepositoryImpl(AggregateRepository aggregateRepository) {
        this.aggregateRepository = aggregateRepository;
    }

    @Override
    public AverageCount getLatestActuatorAverage(String system, String layer, DeviceType actuatorType) {
        return aggregateRepository.getLatestAverage(system, layer, actuatorType.getName(), "actuator_status");
    }

    @Override
    public Map<DeviceType, AverageCount> getLatestActuatorAverages(String system, String layer, List<DeviceType> actuatorTypes) {
        Map<String, AverageCount> result = aggregateRepository.getLatestAverages(
                system,
                layer,
                actuatorTypes.stream().map(DeviceType::getName).toList(),
                "actuator_status");
        return result.entrySet().stream()
                .collect(Collectors.toMap(e -> DeviceType.fromName(e.getKey()), Map.Entry::getValue));
    }
}

