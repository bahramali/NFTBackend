package se.hydroleaf.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.UserRole;

public record UserCreateRequest(
        @NotBlank @Email @Size(max = 128) String email,
        @NotBlank @Size(min = 6, max = 255) String password,
        @Size(max = 128) String displayName,
        @NotNull UserRole role,
        Boolean active,
        Set<Permission> permissions
) {
}
