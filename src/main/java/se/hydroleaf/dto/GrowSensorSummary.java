package se.hydroleaf.dto;

public record GrowSensorSummary(
        StatusAverageResponse light,
        StatusAverageResponse humidity,
        StatusAverageResponse temperature
) {}
