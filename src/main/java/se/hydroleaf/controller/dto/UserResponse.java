package se.hydroleaf.controller.dto;

import java.time.LocalDateTime;
import java.util.Set;
import se.hydroleaf.model.User;
import se.hydroleaf.model.UserRole;

public record UserResponse(
        Long id,
        String username,
        String email,
        String displayName,
        UserRole role,
        boolean active,
        LocalDateTime createdAt,
        Set<String> permissions
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt(),
                user.getPermissions().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet())
        );
    }
}
