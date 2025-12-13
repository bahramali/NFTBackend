package se.hydroleaf.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import java.util.Set;
import se.hydroleaf.model.Permission;

public record AdminInviteRequest(
        @NotBlank @Email @Size(max = 128) String email,
        @Size(max = 128) String displayName,
        Set<Permission> permissions,
        @Positive Integer expiresInHours
) {
}
