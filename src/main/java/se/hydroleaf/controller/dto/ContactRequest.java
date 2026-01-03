package se.hydroleaf.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContactRequest(
        @NotBlank @Size(min = 2, max = 120) String fullName,
        @NotBlank @Email @Size(max = 190) String email,
        @Size(max = 40) String phone,
        @NotNull ContactSubject subject,
        @NotBlank @Size(min = 20, max = 2000) String message,
        @Size(max = 2048) String turnstileToken,
        String companyWebsite
) {
}
