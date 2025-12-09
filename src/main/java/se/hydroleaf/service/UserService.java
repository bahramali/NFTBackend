package se.hydroleaf.service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import se.hydroleaf.controller.dto.UserCreateRequest;
import se.hydroleaf.controller.dto.UserUpdateRequest;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.User;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> listUsers() {
        return userRepository.findAll();
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Transactional
    public User create(UserCreateRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        UserRole role = request.role();
        User user = User.builder()
                .username(request.username().trim())
                .email(request.email().trim())
                .password(request.password())
                .displayName(request.displayName())
                .role(role == null ? UserRole.CUSTOMER : role)
                .permissions(request.permissions() == null ? Set.of() : Set.copyOf(request.permissions()))
                .active(request.active() == null || request.active())
                .build();
        return userRepository.save(user);
    }

    @Transactional
    public User update(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (request.username() != null) {
            String trimmed = request.username().trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("Username cannot be empty");
            }
            if (userRepository.existsByUsernameIgnoreCaseAndIdNot(trimmed, id)) {
                throw new IllegalArgumentException("Username already exists");
            }
            user.setUsername(trimmed);
        }

        if (request.email() != null) {
            String trimmed = request.email().trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("Email cannot be empty");
            }
            if (userRepository.existsByEmailIgnoreCaseAndIdNot(trimmed, id)) {
                throw new IllegalArgumentException("Email already exists");
            }
            user.setEmail(trimmed);
        }

        if (request.password() != null) {
            if (request.password().isBlank()) {
                throw new IllegalArgumentException("Password cannot be blank");
            }
            user.setPassword(request.password());
        }

        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }

        if (request.role() != null) {
            user.setRole(request.role());
        }

        if (request.permissions() != null) {
            user.setPermissions(Set.copyOf(request.permissions()));
        }

        if (request.active() != null) {
            user.setActive(request.active());
        }

        return userRepository.save(user);
    }
}
