package se.hydroleaf.dto;

import java.util.List;

/**
 * DTO representing aggregated readings for a specific sensor type.
 * Field names mirror the underlying database column names.
 */
public record AggregatedSensorData(
        String sensorType,
        String unit,
        List<TimestampValue> data
) {}
