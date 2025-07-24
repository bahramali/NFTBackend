package se.hydroleaf.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SensorDataResponse(
        String sensorId,
        String type,
        Object value,
        String unit,
        String source
) {}
