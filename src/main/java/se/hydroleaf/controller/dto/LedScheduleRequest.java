package se.hydroleaf.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record LedScheduleRequest(
        String system,
        String deviceId,
        String command,
        @NotBlank
        @Pattern(regexp = "^L0?[1-4]$|^L[1-4]$", message = "layer must be between L01 and L04")
        String layer,
        @Min(0) @Max(23) int onHour,
        @Min(0) @Max(59) int onMinute,
        @Positive int durationHours
) {
}
