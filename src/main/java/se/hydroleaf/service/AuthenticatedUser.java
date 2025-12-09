package se.hydroleaf.service;

import java.util.Set;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.UserRole;

public record AuthenticatedUser(
        Long userId,
        UserRole role,
        Set<Permission> permissions
) {
}
