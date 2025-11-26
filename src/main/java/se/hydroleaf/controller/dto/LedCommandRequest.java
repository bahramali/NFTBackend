package se.hydroleaf.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record LedCommandRequest(
        @NotBlank String system,
        @NotBlank
        @Pattern(regexp = "^L0?[1-4]$|^L[1-4]$", message = "layer must be between L01 and L04")
        String layer,
        @NotBlank String deviceId,
        @NotBlank String command,
        @Positive Integer durationSec
) {
}
