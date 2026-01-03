package se.hydroleaf.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.UserRole;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationService.class);
    private final AuthService authService;

    public AuthenticatedUser requireAuthenticated(String token) {
        try {
            AuthenticatedUser user = authService.authenticate(token);
            log.info("Authorization check passed: authenticated userId={} role={}", user.userId(), user.role());
            return user;
        } catch (SecurityException se) {
            log.warn("Authorization check failed: {}", se.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, se.getMessage(), se);
        }
    }

    public void requireRole(AuthenticatedUser user, UserRole... allowedRoles) {
        if (user.role() == UserRole.SUPER_ADMIN) {
            log.info("Role check bypassed for super admin userId={}", user.userId());
            return;
        }
        boolean allowed = Arrays.stream(allowedRoles).anyMatch(role -> role == user.role());
        if (!allowed) {
            log.warn(
                    "Role check failed for userId={} role={} allowedRoles={}",
                    user.userId(),
                    user.role(),
                    Arrays.toString(allowedRoles));
            throw forbidden();
        }
        log.info(
                "Role check passed for userId={} role={} allowedRoles={}",
                user.userId(),
                user.role(),
                Arrays.toString(allowedRoles));
    }

    public void requirePermission(AuthenticatedUser user, Permission... requiredPermissions) {
        if (user.role() == UserRole.SUPER_ADMIN) {
            log.info("Permission check bypassed for super admin userId={}", user.userId());
            return;
        }
        if (user.role() != UserRole.ADMIN) {
            log.warn(
                    "Permission check failed for userId={} role={} requiredPermissions={}",
                    user.userId(),
                    user.role(),
                    Arrays.toString(requiredPermissions));
            throw forbidden();
        }
        Set<Permission> userPermissions = new HashSet<>(user.permissions());
        boolean hasAllPermissions = Arrays.stream(requiredPermissions).allMatch(userPermissions::contains);
        if (!hasAllPermissions) {
            log.warn(
                    "Permission check failed for userId={} role={} requiredPermissions={} userPermissions={}",
                    user.userId(),
                    user.role(),
                    Arrays.toString(requiredPermissions),
                    userPermissions);
            throw forbidden();
        }
        log.info(
                "Permission check passed for userId={} requiredPermissions={}",
                user.userId(),
                Arrays.toString(requiredPermissions));
    }

    public void requireAnyPermission(AuthenticatedUser user, Permission... requiredPermissions) {
        if (user.role() == UserRole.SUPER_ADMIN) {
            log.info("Any-permission check bypassed for super admin userId={}", user.userId());
            return;
        }
        if (user.role() != UserRole.ADMIN) {
            log.warn(
                    "Any-permission check failed for userId={} role={} requiredPermissions={}",
                    user.userId(),
                    user.role(),
                    Arrays.toString(requiredPermissions));
            throw forbidden();
        }
        Set<Permission> userPermissions = new HashSet<>(user.permissions());
        boolean hasAnyPermission = Arrays.stream(requiredPermissions).anyMatch(userPermissions::contains);
        if (!hasAnyPermission) {
            log.warn(
                    "Any-permission check failed for userId={} role={} requiredPermissions={} userPermissions={}",
                    user.userId(),
                    user.role(),
                    Arrays.toString(requiredPermissions),
                    userPermissions);
            throw forbidden();
        }
        log.info(
                "Any-permission check passed for userId={} requiredPermissions={}",
                user.userId(),
                Arrays.toString(requiredPermissions));
    }

    public void requireRoleOrPermission(AuthenticatedUser user, Permission permission, UserRole... allowedRoles) {
        if (user.role() == UserRole.SUPER_ADMIN) {
            log.info("Role-or-permission check bypassed for super admin userId={}", user.userId());
            return;
        }
        boolean allowedRole = Arrays.stream(allowedRoles).anyMatch(role -> role == user.role());
        if (allowedRole) {
            log.info(
                    "Role-or-permission check passed by role for userId={} role={} allowedRoles={}",
                    user.userId(),
                    user.role(),
                    Arrays.toString(allowedRoles));
            return;
        }
        if (user.permissions().contains(permission)) {
            log.info(
                    "Role-or-permission check passed by permission for userId={} permission={}",
                    user.userId(),
                    permission);
            return;
        }
        log.warn(
                "Role-or-permission check failed for userId={} role={} allowedRoles={} permission={}",
                user.userId(),
                user.role(),
                Arrays.toString(allowedRoles),
                permission);
        throw forbidden();
    }

    public void requireSelfAccess(AuthenticatedUser user, Long resourceOwnerId) {
        if (!user.userId().equals(resourceOwnerId)) {
            log.warn(
                    "Self-access check failed for userId={} resourceOwnerId={}",
                    user.userId(),
                    resourceOwnerId);
            throw forbidden();
        }
        log.info("Self-access check passed for userId={} resourceOwnerId={}", user.userId(), resourceOwnerId);
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
