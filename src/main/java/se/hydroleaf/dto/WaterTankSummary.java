package se.hydroleaf.dto;

public record WaterTankSummary(
        StatusAverageResponse waterTemperature,
        StatusAverageResponse dissolvedOxygen,
        StatusAverageResponse pH,
        StatusAverageResponse electricalConductivity
) {}
