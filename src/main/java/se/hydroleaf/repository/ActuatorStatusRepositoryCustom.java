package se.hydroleaf.repository;

public interface ActuatorStatusRepositoryCustom {

    AverageCount getLatestActuatorAverage(String system, String layer, String actuatorType);
}

