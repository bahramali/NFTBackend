package se.hydroleaf.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.UserRole;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final AuthService authService;

    public AuthenticatedUser requireAuthenticated(String token) {
        try {
            return authService.authenticate(token);
        } catch (SecurityException se) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, se.getMessage(), se);
        }
    }

    public void requireRole(AuthenticatedUser user, UserRole... allowedRoles) {
        if (user.role() == UserRole.SUPER_ADMIN) {
            return;
        }
        boolean allowed = Arrays.stream(allowedRoles).anyMatch(role -> role == user.role());
        if (!allowed) {
            throw forbidden();
        }
    }

    public void requirePermission(AuthenticatedUser user, Permission... requiredPermissions) {
        if (user.role() == UserRole.SUPER_ADMIN) {
            return;
        }
        if (user.role() != UserRole.ADMIN) {
            throw forbidden();
        }
        Set<Permission> userPermissions = new HashSet<>(user.permissions());
        boolean hasAllPermissions = Arrays.stream(requiredPermissions).allMatch(userPermissions::contains);
        if (!hasAllPermissions) {
            throw forbidden();
        }
    }

    public void requireAnyPermission(AuthenticatedUser user, Permission... requiredPermissions) {
        if (user.role() == UserRole.SUPER_ADMIN) {
            return;
        }
        if (user.role() != UserRole.ADMIN) {
            throw forbidden();
        }
        Set<Permission> userPermissions = new HashSet<>(user.permissions());
        boolean hasAnyPermission = Arrays.stream(requiredPermissions).anyMatch(userPermissions::contains);
        if (!hasAnyPermission) {
            throw forbidden();
        }
    }

    public void requireRoleOrPermission(AuthenticatedUser user, Permission permission, UserRole... allowedRoles) {
        if (user.role() == UserRole.SUPER_ADMIN) {
            return;
        }
        boolean allowedRole = Arrays.stream(allowedRoles).anyMatch(role -> role == user.role());
        if (allowedRole) {
            return;
        }
        if (user.permissions().contains(permission)) {
            return;
        }
        throw forbidden();
    }

    public void requireSelfAccess(AuthenticatedUser user, Long resourceOwnerId) {
        if (!user.userId().equals(resourceOwnerId)) {
            throw forbidden();
        }
    }

    public void requireAdminOrOperator(AuthenticatedUser user) {
        requireRole(user, UserRole.ADMIN, UserRole.WORKER);
    }

    public AuthenticatedUser requireAdminOrOperator(String token) {
        AuthenticatedUser authenticatedUser = requireAuthenticated(token);
        requireAdminOrOperator(authenticatedUser);
        return authenticatedUser;
    }

    public void requireMonitoringView(AuthenticatedUser user) {
        if (user.role() == UserRole.SUPER_ADMIN || user.role() == UserRole.WORKER) {
            return;
        }
        requirePermission(user, Permission.MONITORING_VIEW);
    }

    public AuthenticatedUser requireMonitoringView(String token) {
        AuthenticatedUser authenticatedUser = requireAuthenticated(token);
        requireMonitoringView(authenticatedUser);
        return authenticatedUser;
    }

    public void requireMonitoringControl(AuthenticatedUser user) {
        if (user.role() == UserRole.SUPER_ADMIN || user.role() == UserRole.WORKER) {
            return;
        }
        requirePermission(user, Permission.MONITORING_CONTROL);
    }

    public AuthenticatedUser requireMonitoringControl(String token) {
        AuthenticatedUser authenticatedUser = requireAuthenticated(token);
        requireMonitoringControl(authenticatedUser);
        return authenticatedUser;
    }

    public void requireMonitoringConfig(AuthenticatedUser user) {
        if (user.role() == UserRole.SUPER_ADMIN || user.role() == UserRole.WORKER) {
            return;
        }
        requirePermission(user, Permission.MONITORING_CONFIG);
    }

    public AuthenticatedUser requireMonitoringConfig(String token) {
        AuthenticatedUser authenticatedUser = requireAuthenticated(token);
        requireMonitoringConfig(authenticatedUser);
        return authenticatedUser;
    }

    public void requireSuperAdmin(AuthenticatedUser user) {
        requireRole(user, UserRole.SUPER_ADMIN);
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }
}
