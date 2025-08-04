package se.hydroleaf.dto;

import java.util.List;

/**
 * DTO representing aggregated readings for a specific sensor name and value type.
 * Fields intentionally use `sensorName` and `valueType` to mirror the underlying
 * database column names `sensor_name` and `value_type`.
 */
public record AggregatedSensorData(
        String sensorName,
        String valueType,
        String unit,
        List<TimestampValue> data
) {}
