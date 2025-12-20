package se.hydroleaf.controller.dto;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.User;
import se.hydroleaf.model.UserRole;

public record MyProfileResponse(
        Long id,
        String email,
        String displayName,
        UserRole role,
        List<String> permissions
) {

    public static MyProfileResponse from(User user, Set<Permission> permissions) {
        List<String> permissionNames = permissions == null
                ? List.of()
                : permissions.stream().map(Enum::name).collect(Collectors.toList());
        return new MyProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                permissionNames
        );
    }
}
