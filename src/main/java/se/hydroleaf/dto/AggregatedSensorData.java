package se.hydroleaf.dto;

import java.util.List;

public record AggregatedSensorData(
        String sensorId,
        String type,
        String unit,
        List<TimestampValue> data
) {}
