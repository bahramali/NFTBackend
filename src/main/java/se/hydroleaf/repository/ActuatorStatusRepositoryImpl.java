package se.hydroleaf.repository;

import org.springframework.stereotype.Repository;

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
}

