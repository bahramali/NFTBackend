package se.hydroleaf.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StatusAllAverageResponse(
        StatusAverageResponse light,
        StatusAverageResponse humidity,
        StatusAverageResponse temperature,
        StatusAverageResponse dissolvedOxygen,
        StatusAverageResponse airpump
) {}
