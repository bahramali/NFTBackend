package se.hydroleaf.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.hydroleaf.config.AuthProperties;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.RefreshToken;
import se.hydroleaf.model.User;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.repository.RefreshTokenRepository;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthProperties authProperties;
    private final Clock clock = Clock.systemUTC();

    @Transactional
    public String createRefreshToken(User user, String userAgent, String ip) {
        return createRefreshTokenSession(user, userAgent, ip).refreshToken();
    }

    @Transactional
    public RefreshTokenSession rotateRefreshToken(String refreshToken, String userAgent, String ip) {
        RefreshToken existing = requireValidToken(refreshToken);
        Instant now = Instant.now(clock);
        existing.setRevokedAt(now);
        refreshTokenRepository.save(existing);
        return createRefreshTokenSession(existing.getUser(), userAgent, ip);
    }

    @Transactional
    public void revokeRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        String hash = hashToken(refreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now(clock));
                refreshTokenRepository.save(token);
            }
        });
    }

    @Transactional(readOnly = true)
    public RefreshToken requireValidToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new SecurityException("Missing refresh token");
        }
        String hash = hashToken(refreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHashWithUserAndPermissions(hash)
                .orElseThrow(() -> new SecurityException("Invalid refresh token"));
        Instant now = Instant.now(clock);
        if (token.getRevokedAt() != null) {
            throw new SecurityException("Refresh token revoked");
        }
        if (token.getExpiresAt().isBefore(now)) {
            throw new SecurityException("Refresh token expired");
        }
        return token;
    }

    private RefreshTokenSession createRefreshTokenSession(User user, String userAgent, String ip) {
        String token = TokenGenerator.randomVerifier();
        Instant now = Instant.now(clock);
        Set<Permission> permissions = user.getPermissions();
        UserRole role = user.getRole();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(token))
                .issuedAt(now)
                .expiresAt(now.plus(authProperties.getRefresh().getTokenTtl()))
                .userAgent(userAgent)
                .ip(ip)
                .build();
        refreshTokenRepository.save(refreshToken);
        return new RefreshTokenSession(token, user.getId(), role, permissions);
    }

    private String hashToken(String refreshToken) {
        return TokenGenerator.sha256Base64Url(refreshToken);
    }

    public record RefreshTokenSession(
            String refreshToken,
            long userId,
            UserRole role,
            Set<Permission> permissions
    ) {}
}
