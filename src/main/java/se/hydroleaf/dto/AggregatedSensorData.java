package se.hydroleaf.dto;

import java.util.List;

/**
 * DTO representing aggregated readings for a specific sensor type and value type.
 * Fields intentionally use `sensorType` and `valueType` to mirror the underlying
 * database column names `sensor_type` and `value_type`.
 */
public record AggregatedSensorData(
        String sensorType,
        String valueType,
        String unit,
        List<TimestampValue> data
) {}
