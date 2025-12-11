package se.hydroleaf.service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.User;
import se.hydroleaf.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
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
        Optional<User> byEmail = userRepository.findByEmailIgnoreCase(email);
        User user = byEmail.orElseThrow(() -> new SecurityException("Invalid credentials"));

        if (!user.isActive()) {
            throw new SecurityException("User is inactive");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new SecurityException("Invalid credentials");
        }
        String token = UUID.randomUUID().toString();
        Set<Permission> permissions = user.getPermissions();
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(user.getId(), user.getRole(), permissions);
        tokens.put(token, authenticatedUser);
        return new LoginResult(token, authenticatedUser);
    }

    public BCryptPasswordEncoder passwordEncoder() {
        return passwordEncoder;
    }

    public record LoginResult(String token, AuthenticatedUser user) {}
}
