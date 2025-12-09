package se.hydroleaf.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.UserRole;

@Service
public class AuthorizationService {

    public void requireRole(AuthenticatedUser user, UserRole expected) {
        if (user == null) {
            throw forbidden();
        }
        if (user.role() == UserRole.SUPER_ADMIN) {
            return;
        }
        if (user.role() != expected) {
            throw forbidden();
        }
    }

    public void requirePermission(AuthenticatedUser user, Permission permission) {
        if (user.role() == UserRole.SUPER_ADMIN) {
            return;
        }
        if (user.role() != UserRole.ADMIN || !user.permissions().contains(permission)) {
            throw forbidden();
        }
    }

    public void requireSelfAccess(AuthenticatedUser user, Long resourceOwnerId) {
        if (!user.userId().equals(resourceOwnerId)) {
            throw forbidden();
        }
    }

    public void requireSuperAdmin(AuthenticatedUser user) {
        if (user.role() != UserRole.SUPER_ADMIN) {
            throw forbidden();
        }
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }
}
