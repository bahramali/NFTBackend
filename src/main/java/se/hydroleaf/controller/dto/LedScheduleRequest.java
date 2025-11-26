package se.hydroleaf.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record LedScheduleRequest(
        @NotBlank String system,
        @NotBlank String deviceId,
        @NotBlank String command,
        @Min(0) @Max(23) int onHour,
        @Min(0) @Max(59) int onMinute,
        @Positive int durationHours
) {
}
