package se.hydroleaf.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank String token,
        @NotBlank
        @Size(min = 8, max = 255)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\p{Alnum}]).+$",
                message = "Password must include at least one uppercase letter, one lowercase letter, one number, and one special character"
        )
        String password
) {
}
