package se.hydroleaf.repository;

import java.util.List;
import java.util.Map;

public interface ActuatorStatusRepositoryCustom {

    AverageCount getLatestActuatorAverage(String system, String layer, String actuatorType);

    Map<String, AverageCount> getLatestActuatorAverages(String system, String layer, List<String> actuatorTypes);
}

