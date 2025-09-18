package se.hydroleaf.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RefreshTokenService {

    private static final class StoredToken {
        private final String email;
        private final Instant expiresAt;

        private StoredToken(String email, Instant expiresAt) {
            this.email = email;
            this.expiresAt = expiresAt;
        }
    }

    private final Map<String, StoredToken> refreshTokens = new ConcurrentHashMap<>();
    private final JwtProvider jwtProvider;

    public RefreshTokenService(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    public String createRefreshToken(String email) {
        refreshTokens.entrySet().removeIf(entry -> Objects.equals(entry.getValue().email, email));
        String token = jwtProvider.generateRefreshToken(email);
        Instant expiresAt = Instant.now().plus(jwtProvider.getRefreshTokenValidity());
        refreshTokens.put(token, new StoredToken(email, expiresAt));
        return token;
    }

    public String validateAndGetEmail(String token) {
        StoredToken storedToken = refreshTokens.get(token);
        if (storedToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has been revoked");
        }
        if (storedToken.expiresAt.isBefore(Instant.now())) {
            refreshTokens.remove(token);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has expired");
        }
        String email;
        try {
            if (!jwtProvider.validateRefreshToken(token)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
            }
            email = jwtProvider.getEmailFromRefreshToken(token);
        } catch (RuntimeException ex) {
            refreshTokens.remove(token);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token", ex);
        }
        if (!storedToken.email.equals(email)) {
            refreshTokens.remove(token);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token does not match user");
        }
        return email;
    }

    public void revokeToken(String token) {
        refreshTokens.remove(token);
    }

    public void revokeTokensForUser(String email) {
        refreshTokens.entrySet().removeIf(entry -> Objects.equals(entry.getValue().email, email));
    }
}
