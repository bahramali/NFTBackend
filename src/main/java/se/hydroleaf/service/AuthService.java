package se.hydroleaf.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
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
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthenticatedUser authenticate(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            log.warn("Authentication failed: missing or invalid authorization header");
            throw new SecurityException("Missing or invalid authorization header");
        }
        String token = bearerToken.substring("Bearer ".length()).trim();
        AuthenticatedUser authenticatedUser = jwtService.parseAccessToken(token);
        log.info(
                "Authentication succeeded (tokenPrefix={}, userId={}, role={})",
                tokenPrefix(token),
                authenticatedUser.userId(),
                authenticatedUser.role());
        return authenticatedUser;
    }

    public LoginResult login(String email, String password) {
        return login(email, password, null, null);
    }

    public LoginResult login(String email, String password, String userAgent, String ip) {
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
        return createSession(user, userAgent, ip);
    }

    public LoginResult createSession(User user) {
        return createSession(user, null, null);
    }

    public LoginResult createSession(User user, String userAgent, String ip) {
        if (user.getStatus() == UserStatus.INVITED || user.getStatus() == UserStatus.DISABLED) {
            log.warn("Session creation blocked: user status is {} for userId={}", user.getStatus(), user.getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not allowed to login");
        }
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        Set<Permission> permissions = user.getPermissions();
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(user.getId(), user.getRole(), permissions);
        String accessToken = jwtService.createAccessToken(authenticatedUser);
        String refreshToken = refreshTokenService.createRefreshToken(user, userAgent, ip);
        log.info(
                "Session created (tokenPrefix={}, userId={}, role={}, permissions={})",
                tokenPrefix(accessToken),
                authenticatedUser.userId(),
                authenticatedUser.role(),
                permissions);
        return new LoginResult(accessToken, refreshToken, authenticatedUser);
    }

    public RefreshResult refreshAccessToken(String refreshToken, String userAgent, String ip) {
        RefreshTokenService.RefreshTokenSession session = refreshTokenService.rotateRefreshToken(refreshToken, userAgent, ip);
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                session.userId(),
                session.role(),
                session.permissions()
        );
        String accessToken = jwtService.createAccessToken(authenticatedUser);
        return new RefreshResult(accessToken, session.refreshToken());
    }

    public void logout(String refreshToken) {
        refreshTokenService.revokeRefreshToken(refreshToken);
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

    public record LoginResult(String accessToken, String refreshToken, AuthenticatedUser user) {}

    public record RefreshResult(String accessToken, String refreshToken) {}

    private String tokenPrefix(String token) {
        if (token == null || token.isBlank()) {
            return "n/a";
        }
        int prefixLength = Math.min(8, token.length());
        return token.substring(0, prefixLength);
    }
}
