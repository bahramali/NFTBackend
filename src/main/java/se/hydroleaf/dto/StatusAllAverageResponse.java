package se.hydroleaf.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StatusAllAverageResponse(
        StatusAverageResponse lux,
        StatusAverageResponse humidity,
        StatusAverageResponse temperature,
        @JsonProperty("do") StatusAverageResponse dissolvedOxygen,
        StatusAverageResponse airpump
) {}
