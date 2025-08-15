package se.hydroleaf.dto;

public record LayerSensorSummary(
        StatusAverageResponse light,
        StatusAverageResponse humidity,
        StatusAverageResponse temperature,
        StatusAverageResponse dissolvedOxygen
) {}
