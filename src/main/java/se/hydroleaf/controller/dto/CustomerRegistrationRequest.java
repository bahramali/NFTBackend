package se.hydroleaf.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerRegistrationRequest(
        @NotBlank String email,
        @NotBlank String password,
        String displayName
) {
}
