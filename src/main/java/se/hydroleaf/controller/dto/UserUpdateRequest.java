package se.hydroleaf.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import se.hydroleaf.model.UserRole;

public record UserUpdateRequest(
        @Size(max = 64) String username,
        @Email @Size(max = 128) String email,
        @Size(min = 6, max = 255) String password,
        @Size(max = 128) String displayName,
        UserRole role,
        Boolean active
) {
}
