package se.hydroleaf.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.User;
import se.hydroleaf.model.UserStatus;
import se.hydroleaf.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConcurrentHashMap<String, AuthenticatedUser> tokens = new ConcurrentHashMap<>();

    public AuthenticatedUser authenticate(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            log.warn("Authentication failed: missing or invalid authorization header");
            throw new SecurityException("Missing or invalid authorization header");
        }
        String token = bearerToken.substring("Bearer ".length()).trim();
        AuthenticatedUser authenticatedUser = tokens.get(token);
        if (authenticatedUser == null) {
            log.warn("Authentication failed: invalid or expired token (tokenPrefix={})", tokenPrefix(token));
            throw new SecurityException("Invalid or expired token");
        }
        log.info(
                "Authentication succeeded (tokenPrefix={}, userId={}, role={})",
                tokenPrefix(token),
                authenticatedUser.userId(),
                authenticatedUser.role());
        return authenticatedUser;
    }

    public LoginResult login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        Optional<User> byEmail = userRepository.findByEmailIgnoreCase(normalizedEmail);
        User user = byEmail.orElseThrow(() -> {
            log.warn("Login failed: user not found for email={}", normalizedEmail);
            return new SecurityException("Invalid credentials");
        });

        if (user.getStatus() == UserStatus.INVITED || user.getStatus() == UserStatus.DISABLED) {
            log.warn("Login blocked: user status is {} for email={}", user.getStatus(), normalizedEmail);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not allowed to login");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Login failed: invalid password for email={}", normalizedEmail);
            throw new SecurityException("Invalid credentials");
        }
        return createSession(user);
    }

    public LoginResult createSession(User user) {
        if (user.getStatus() == UserStatus.INVITED || user.getStatus() == UserStatus.DISABLED) {
            log.warn("Session creation blocked: user status is {} for userId={}", user.getStatus(), user.getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not allowed to login");
        }
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        String token = UUID.randomUUID().toString();
        Set<Permission> permissions = user.getPermissions();
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(user.getId(), user.getRole(), permissions);
        tokens.put(token, authenticatedUser);
        log.info(
                "Session created (tokenPrefix={}, userId={}, role={}, permissions={})",
                tokenPrefix(token),
                authenticatedUser.userId(),
                authenticatedUser.role(),
                permissions);
        return new LoginResult(token, authenticatedUser);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw new SecurityException("Invalid credentials");
        }
        String trimmed = email.trim().toLowerCase();
        if (trimmed.isEmpty()) {
            throw new SecurityException("Invalid credentials");
        }
        return trimmed;
    }

    public record LoginResult(String token, AuthenticatedUser user) {}

    private String tokenPrefix(String token) {
        if (token == null || token.isBlank()) {
            return "n/a";
        }
        int prefixLength = Math.min(8, token.length());
        return token.substring(0, prefixLength);
    }
}
