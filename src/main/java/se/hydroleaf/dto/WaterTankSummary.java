package se.hydroleaf.dto;

public record WaterTankSummary(
        StatusAverageResponse dissolvedTemp,
        StatusAverageResponse dissolvedOxygen,
        StatusAverageResponse pH,
        StatusAverageResponse dissolvedEC,
        StatusAverageResponse dissolvedTDS
) {}
