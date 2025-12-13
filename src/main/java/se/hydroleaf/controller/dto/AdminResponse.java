package se.hydroleaf.controller.dto;

import java.util.Set;
import se.hydroleaf.model.User;
import se.hydroleaf.model.UserStatus;

public record AdminResponse(
        Long id,
        String email,
        String displayName,
        boolean active,
        UserStatus status,
        Set<String> permissions
) {
    public static AdminResponse from(User user) {
        return new AdminResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.isActive(),
                user.getStatus(),
                user.getPermissions().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet())
        );
    }
}
