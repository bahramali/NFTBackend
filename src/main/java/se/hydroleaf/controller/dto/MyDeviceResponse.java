package se.hydroleaf.controller.dto;

import java.time.Instant;
import java.util.List;

public record MyDeviceResponse(
        String deviceId,
        String name,
        boolean online,
        Instant lastSeen,
        List<MyDeviceMetricResponse> lastMetrics
) {
}
