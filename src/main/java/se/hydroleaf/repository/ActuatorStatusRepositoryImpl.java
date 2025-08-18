package se.hydroleaf.repository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class ActuatorStatusRepositoryImpl implements ActuatorStatusRepositoryCustom {

    private final AggregateRepository aggregateRepository;

    public ActuatorStatusRepositoryImpl(AggregateRepository aggregateRepository) {
        this.aggregateRepository = aggregateRepository;
    }

    @Override
    public AverageCount getLatestActuatorAverage(String system, String layer, String actuatorType) {
        return aggregateRepository.getLatestAverage(system, layer, actuatorType, "actuator_status");
    }

    @Override
    public Map<String, AverageCount> getLatestActuatorAverages(String system, String layer, List<String> actuatorTypes) {
        return aggregateRepository.getLatestAverages(system, layer, actuatorTypes, "actuator_status");
    }
}

