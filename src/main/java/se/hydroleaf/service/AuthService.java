package se.hydroleaf.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConcurrentHashMap<String, AuthenticatedUser> tokens = new ConcurrentHashMap<>();

    public AuthenticatedUser authenticate(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new SecurityException("Missing or invalid authorization header");
        }
        String token = bearerToken.substring("Bearer ".length()).trim();
        AuthenticatedUser authenticatedUser = tokens.get(token);
        if (authenticatedUser == null) {
            throw new SecurityException("Invalid or expired token");
        }
        return authenticatedUser;
    }

    public LoginResult login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        Optional<User> byEmail = userRepository.findByEmailIgnoreCase(normalizedEmail);
        User user = byEmail.orElseThrow(() -> new SecurityException("Invalid credentials"));

        if (user.getStatus() == UserStatus.INVITED || user.getStatus() == UserStatus.DISABLED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not allowed to login");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new SecurityException("Invalid credentials");
        }
        return createSession(user);
    }

    public LoginResult createSession(User user) {
        if (user.getStatus() == UserStatus.INVITED || user.getStatus() == UserStatus.DISABLED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not allowed to login");
        }
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        String token = UUID.randomUUID().toString();
        Set<Permission> permissions = user.getPermissions();
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(user.getId(), user.getRole(), permissions);
        tokens.put(token, authenticatedUser);
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
}
