package se.hydroleaf.repository;

import java.util.List;
import java.util.Map;

import se.hydroleaf.model.DeviceType;

public interface SensorDataRepositoryCustom {

    AverageCount getLatestAverage(String system, String layer, DeviceType sensorType);

    Map<DeviceType, AverageCount> getLatestAverages(String system, String layer, List<DeviceType> sensorTypes);
}

