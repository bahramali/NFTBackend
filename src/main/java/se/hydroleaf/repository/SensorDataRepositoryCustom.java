package se.hydroleaf.repository;

public interface SensorDataRepositoryCustom {
    AverageCount getLatestAverage(String system, String layer, String sensorType);
}

