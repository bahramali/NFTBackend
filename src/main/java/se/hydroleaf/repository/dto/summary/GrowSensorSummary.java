package se.hydroleaf.repository.dto.summary;

public record GrowSensorSummary(
        StatusAverageResponse light,
        StatusAverageResponse humidity,
        StatusAverageResponse temperature
) {}
