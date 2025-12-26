package se.hydroleaf.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.Set;
import se.hydroleaf.model.AdminPreset;
import se.hydroleaf.model.Permission;

public record AdminInviteRequest(
        @NotBlank @Email @Size(max = 128) String email,
        @Size(max = 128) String displayName,
        AdminPreset preset,
        Set<Permission> permissions,
        @Positive Integer expiresInHours,
        @Future OffsetDateTime expiresAt
) {
}
