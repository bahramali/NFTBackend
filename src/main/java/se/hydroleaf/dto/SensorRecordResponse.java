package se.hydroleaf.dto;

import java.time.Instant;
import java.util.List;

public record SensorRecordResponse(
        Instant timestamp,
        List<SensorDataResponse> sensors
) {}
