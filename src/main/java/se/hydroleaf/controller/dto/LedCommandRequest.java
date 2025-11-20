package se.hydroleaf.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record LedCommandRequest(
        @NotBlank String system,
        @NotBlank String layer,
        @NotBlank String deviceId,
        @NotBlank String controller,
        @NotBlank String command,
        @Positive Integer durationSec
) {
}
