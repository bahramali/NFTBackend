package se.hydroleaf.repository.dto;

import java.time.Instant;

public record LiveNowRow(
        String system,
        String layer,
        String sensorType,
        String unit,
        Double avgValue,
        Long deviceCount,
        Instant recordTime
) {}