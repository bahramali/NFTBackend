package se.hydroleaf.repository.dto.snapshot;

import java.time.Instant;

public record SensorAggregationRow(
        String sensorType,
        String unit,
        Instant bucketTime,
        Double avgValue
) {}
