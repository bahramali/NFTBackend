package se.hydroleaf.dto.summary;

public record GrowSensorSummary(
        StatusAverageResponse light,
        StatusAverageResponse humidity,
        StatusAverageResponse temperature
) {}
