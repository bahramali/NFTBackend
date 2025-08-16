package se.hydroleaf.repository;

import org.springframework.stereotype.Repository;

@Repository
public class SensorDataRepositoryImpl implements SensorDataRepositoryCustom {

    private final AggregateRepository aggregateRepository;

    public SensorDataRepositoryImpl(AggregateRepository aggregateRepository) {
        this.aggregateRepository = aggregateRepository;
    }

    @Override
    public AverageCount getLatestAverage(String system, String layer, String sensorType) {
        return aggregateRepository.getLatestAverage(system, layer, sensorType, "sensor_data");
    }
}

