package se.hydroleaf.dto;

import java.util.List;

public record AggregatedSensorData(
        String sensorName,
        String valueType,
        String unit,
        List<TimestampValue> data
) {}
