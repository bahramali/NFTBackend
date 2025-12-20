package se.hydroleaf.controller.dto;

import java.time.Instant;

public record MyDeviceMetricResponse(
        String sensorType,
        Double value,
        String unit,
        Instant valueTime
) {
}
