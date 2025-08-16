package se.hydroleaf.dto.summary;

import java.util.Map;

public record StatusAllAverageResponse(
        Map<String, StatusAverageResponse> growSensors,
        Map<String, StatusAverageResponse> waterTank,
        StatusAverageResponse airpump
) {}
