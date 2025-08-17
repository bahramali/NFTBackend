package se.hydroleaf.repository;

import java.util.Map;

public interface SensorDataRepositoryCustom {
    AverageCount getLatestAverage(String system, String layer, String sensorType);
    Map<String, AverageCount> getLatestAverages(String system, String layer);
}

