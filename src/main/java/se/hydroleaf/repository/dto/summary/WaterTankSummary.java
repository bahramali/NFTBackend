package se.hydroleaf.repository.dto.summary;

public record WaterTankSummary(
        StatusAverageResponse dissolvedTemp,
        StatusAverageResponse dissolvedOxygen,
        StatusAverageResponse pH,
        StatusAverageResponse dissolvedEC,
        StatusAverageResponse dissolvedTDS
) {}
