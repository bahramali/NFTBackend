package se.hydroleaf.dto;

public record WaterTankSummary(
        StatusAverageResponse dissolvedTemp,
        StatusAverageResponse dissolvedOxygen,
        StatusAverageResponse dissolvedPH,
        StatusAverageResponse dissolvedEC
) {}
