package se.hydroleaf.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String credential,
        @NotBlank String password
) {
}
