package se.hydroleaf.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.model.User;
import se.hydroleaf.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Duration RESET_COOLDOWN = Duration.ofMinutes(5);
    private static final Duration RESET_EXPIRY = Duration.ofHours(1);
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordResetEmailService passwordResetEmailService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final Map<Long, Instant> lastResetRequests = new ConcurrentHashMap<>();

    public void requestPasswordResetForEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return;
        }
        Instant now = clock.instant();
        Instant lastRequest = lastResetRequests.get(user.getId());
        if (lastRequest != null && lastRequest.plus(RESET_COOLDOWN).isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Password reset already requested");
        }
        lastResetRequests.put(user.getId(), now);
        String resetToken = UUID.randomUUID().toString();
        String resetTokenHash = hashToken(resetToken);
        LocalDateTime expiresAt = LocalDateTime.ofInstant(now.plus(RESET_EXPIRY), clock.getZone());
        user.setPasswordResetTokenHash(resetTokenHash);
        user.setPasswordResetExpiresAt(expiresAt);
        user.setPasswordResetUsedAt(null);
        userRepository.save(user);
        passwordResetEmailService.sendPasswordResetEmail(user.getEmail(), resetToken);
    }

    public void confirmPasswordReset(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token is required");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
        }
        String tokenHash = hashToken(token);
        User user = userRepository.findByPasswordResetTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid password reset token"));
        LocalDateTime now = LocalDateTime.now(clock);
        if (user.getPasswordResetExpiresAt() != null && user.getPasswordResetExpiresAt().isBefore(now)) {
            throw new ResponseStatusException(HttpStatus.GONE, "Password reset token has expired");
        }
        if (user.getPasswordResetUsedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Password reset token already used");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetUsedAt(now);
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);
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
