package se.hydroleaf.dto;

public record StatusAllAverageResponse(
        StatusAverageResponse light,
        StatusAverageResponse humidity,
        StatusAverageResponse temperature,
        StatusAverageResponse dissolvedOxygen,
        StatusAverageResponse airpump
) {}
