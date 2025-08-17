package se.hydroleaf.repository;

import org.springframework.stereotype.Repository;

import java.util.Map;

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

    @Override
    public Map<String, AverageCount> getLatestAverages(String system, String layer) {
        return aggregateRepository.getLatestAverages(system, layer, "sensor_data");
    }
}

