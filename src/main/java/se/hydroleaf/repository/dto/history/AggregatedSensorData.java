package se.hydroleaf.repository.dto.history;

import java.util.List;

public record AggregatedSensorData(
        String sensorType,
        String unit,
        List<TimestampValue> data
) {}
