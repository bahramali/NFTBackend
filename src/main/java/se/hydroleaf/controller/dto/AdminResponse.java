package se.hydroleaf.controller.dto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import se.hydroleaf.model.User;
import se.hydroleaf.model.UserStatus;

public record AdminResponse(
        Long id,
        String email,
        String displayName,
        boolean active,
        UserStatus status,
        LocalDateTime invitedAt,
        LocalDateTime inviteExpiresAt,
        LocalDateTime inviteUsedAt,
        LocalDateTime lastLoginAt,
        Set<String> permissions
) {
    public static AdminResponse from(User user) {
        return new AdminResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.isActive(),
                user.getStatus(),
                user.getInvitedAt(),
                user.getInviteExpiresAt(),
                user.getInviteUsedAt(),
                user.getLastLoginAt(),
                safePermissions(user)
        );
    }

    private static Set<String> safePermissions(User user) {
        Set<se.hydroleaf.model.Permission> permissions = user.getPermissions();
        if (permissions == null) {
            return Collections.emptySet();
        }
        return permissions.stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());
    }
}
