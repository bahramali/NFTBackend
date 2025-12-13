package se.hydroleaf.service;

import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.User;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.model.UserStatus;
import se.hydroleaf.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AdminLifecycleService {

    private static final int DEFAULT_EXPIRY_HOURS = 48;
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InviteEmailService inviteEmailService;

    public record InviteResult(User user, String token) {
    }

    public java.util.List<User> listAdmins() {
        return userRepository.findAllByRole(UserRole.ADMIN);
    }

    @Transactional
    public InviteResult inviteAdmin(
            String email,
            String displayName,
            Set<Permission> permissions,
            Integer expiresInHours,
            java.time.OffsetDateTime expiresAt) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        String token = generateToken();
        String tokenHash = hashToken(token);
        LocalDateTime resolvedExpiresAt = resolveExpiry(expiresInHours, expiresAt);
        LocalDateTime invitedAt = LocalDateTime.now();

        User admin = User.builder()
                .email(normalizedEmail)
                .password(randomPassword())
                .displayName(displayName)
                .role(UserRole.ADMIN)
                .permissions(resolvePermissions(permissions))
                .status(UserStatus.INVITED)
                .invited(true)
                .invitedAt(invitedAt)
                .inviteTokenHash(tokenHash)
                .inviteExpiresAt(resolvedExpiresAt)
                .active(false)
                .build();

        try {
            userRepository.save(admin);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists", e);
        }
        inviteEmailService.sendInviteEmail(admin.getEmail(), token, resolvedExpiresAt);
        return new InviteResult(admin, token);
    }

    private LocalDateTime resolveExpiry(Integer expiresInHours, java.time.OffsetDateTime expiresAt) {
        if (expiresAt != null) {
            LocalDateTime candidate = expiresAt.toLocalDateTime();
            if (candidate.isBefore(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "expiresAt must be in the future");
            }
            return candidate;
        }
        int hours = (expiresInHours == null || expiresInHours <= 0) ? DEFAULT_EXPIRY_HOURS : expiresInHours;
        return LocalDateTime.now().plusHours(hours);
    }

    @Transactional
    public InviteResult resendInvite(Long adminId, Integer expiresInHours) {
        User admin = requireAdmin(adminId);
        if (admin.getStatus() != UserStatus.INVITED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only invited admins can receive a new invite");
        }
        int hours = (expiresInHours == null || expiresInHours <= 0) ? DEFAULT_EXPIRY_HOURS : expiresInHours;
        String token = generateToken();
        String tokenHash = hashToken(token);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(hours);
        LocalDateTime invitedAt = LocalDateTime.now();
        admin.setInviteTokenHash(tokenHash);
        admin.setInviteExpiresAt(expiresAt);
        admin.setInvitedAt(invitedAt);
        admin.setInvited(true);
        admin.setStatus(UserStatus.INVITED);
        admin.setInviteUsedAt(null);
        admin.setActive(false);
        userRepository.save(admin);
        inviteEmailService.sendInviteEmail(admin.getEmail(), token, expiresAt);
        return new InviteResult(admin, token);
    }

    @Transactional
    public User updatePermissions(Long adminId, Set<Permission> permissions) {
        User admin = requireAdmin(adminId);
        admin.setPermissions(resolvePermissions(permissions));
        return userRepository.save(admin);
    }

    @Transactional
    public User updateStatus(Long adminId, UserStatus status, Boolean active) {
        UserStatus targetStatus = resolveStatus(status, active);
        if (targetStatus == UserStatus.INVITED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status cannot be set to INVITED manually");
        }
        User admin = requireAdmin(adminId);
        if (admin.getStatus() == UserStatus.INVITED && targetStatus == UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invited admin must accept invite to activate");
        }
        if (targetStatus == UserStatus.DISABLED) {
            clearInviteMetadata(admin);
        }
        admin.setStatus(targetStatus);
        admin.setActive(targetStatus == UserStatus.ACTIVE);
        return userRepository.save(admin);
    }

    private UserStatus resolveStatus(UserStatus status, Boolean active) {
        if (status != null) {
            return status;
        }
        if (active != null) {
            return active ? UserStatus.ACTIVE : UserStatus.DISABLED;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
    }

    @Transactional
    public void deleteAdmin(Long adminId) {
        User admin = requireAdmin(adminId);
        userRepository.delete(admin);
    }

    @Transactional
    public User acceptInvite(String token, String password) {
        String tokenHash = hashToken(token);
        User admin = userRepository.findByInviteTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token"));
        if (admin.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        if (admin.getStatus() != UserStatus.INVITED || !admin.isInvited()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        if (admin.getInviteExpiresAt() == null || admin.getInviteExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invite has expired");
        }
        validatePassword(password);

        admin.setPassword(passwordEncoder.encode(password));
        admin.setStatus(UserStatus.ACTIVE);
        admin.setActive(true);
        admin.setInvited(false);
        admin.setInviteTokenHash(null);
        admin.setInviteExpiresAt(null);
        admin.setInviteUsedAt(LocalDateTime.now());
        return userRepository.save(admin);
    }

    private void clearInviteMetadata(User admin) {
        admin.setInvited(false);
        admin.setInviteTokenHash(null);
        admin.setInviteExpiresAt(null);
        admin.setInviteUsedAt(null);
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least %d characters".formatted(MIN_PASSWORD_LENGTH));
        }
    }

    private User requireAdmin(Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));
        if (admin.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only ADMIN users can be managed here");
        }
        return admin;
    }

    private Set<Permission> resolvePermissions(Set<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return EnumSet.noneOf(Permission.class);
        }
        return EnumSet.copyOf(permissions);
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

    private String randomPassword() {
        return passwordEncoder.encode(generateToken());
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(Objects.requireNonNull(token).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to hash token", e);
        }
    }
}
