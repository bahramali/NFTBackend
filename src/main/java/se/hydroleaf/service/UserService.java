package se.hydroleaf.service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.controller.dto.UserCreateRequest;
import se.hydroleaf.controller.dto.UserUpdateRequest;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.User;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.repository.UserRepository;
import org.springframework.http.HttpStatus;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;

    public List<User> listUsers() {
        return userRepository.findAll();
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional
    public User create(UserCreateRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        UserRole role = request.role();
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is required");
        }
        rejectSuperAdminRole(role);

        User user = User.builder()
                .email(normalizedEmail)
                .password(hashPassword(request.password()))
                .displayName(request.displayName())
                .role(role)
                .permissions(resolvePermissions(role, request.permissions()))
                .active(request.active() == null || request.active())
                .build();
        return userRepository.save(user);
    }

    @Transactional
    public User update(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        UserRole targetRole = request.role() != null ? request.role() : user.getRole();

        if (request.role() != null) {
            rejectSuperAdminRole(request.role());
        }

        if (request.email() != null) {
            String normalizedEmail = normalizeEmail(request.email());
            if (userRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
            }
            user.setEmail(normalizedEmail);
        }

        if (request.password() != null) {
            if (request.password().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be blank");
            }
            user.setPassword(hashPassword(request.password()));
        }

        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }

        if (request.role() != null) {
            user.setRole(request.role());
            targetRole = request.role();
        }

        if (targetRole != UserRole.ADMIN) {
            user.setPermissions(Set.of());
        } else if (request.permissions() != null) {
            user.setPermissions(resolvePermissions(targetRole, request.permissions()));
        }

        if (request.active() != null) {
            user.setActive(request.active());
        }

        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        userRepository.delete(user);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        String normalized = email.trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email cannot be empty");
        }
        return normalized;
    }

    private String hashPassword(String password) {
        if (password == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        if (password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be blank");
        }
        return authService.passwordEncoder().encode(password);
    }

    private Set<Permission> resolvePermissions(UserRole role, Set<Permission> requestedPermissions) {
        if (role != UserRole.ADMIN) {
            return Set.of();
        }
        if (requestedPermissions == null) {
            return Set.of();
        }
        return Set.copyOf(requestedPermissions);
    }

    private void rejectSuperAdminRole(UserRole role) {
        if (role == UserRole.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "SUPER_ADMIN role cannot be assigned via API");
        }
    }
}
