package se.hydroleaf.repository;

import java.util.List;
import java.util.Map;

import se.hydroleaf.model.DeviceType;

public interface ActuatorStatusRepositoryCustom {

    AverageCount getLatestActuatorAverage(String system, String layer, DeviceType actuatorType);

    Map<DeviceType, AverageCount> getLatestActuatorAverages(String system, String layer, List<DeviceType> actuatorTypes);
}

