package se.hydroleaf.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class JwtProvider {

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final String secret;
    private final Duration accessTokenValidity;
    private final Duration refreshTokenValidity;

    private SecretKey secretKey;
    private JwtParser jwtParser;

    public JwtProvider(@Value("${security.jwt.secret:ChangeMeChangeMeChangeMeChangeMe!}") String secret,
                       @Value("${security.jwt.access-token-validity:PT15M}") Duration accessTokenValidity,
                       @Value("${security.jwt.refresh-token-validity:PT7D}") Duration refreshTokenValidity) {
        this.secret = secret;
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
    }

    @PostConstruct
    void init() {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(secretBytes);
        this.jwtParser = Jwts.parser()
                .verifyWith(secretKey)
                .build();
    }

    public String generateAccessToken(UserDetails userDetails) {
        Instant now = Instant.now();
        return buildToken(userDetails.getUsername(), now, accessTokenValidity, TYPE_ACCESS,
                Map.of(CLAIM_ROLES, extractAuthorities(userDetails.getAuthorities())));
    }

    public String generateRefreshToken(String email) {
        Instant now = Instant.now();
        return buildToken(email, now, refreshTokenValidity, TYPE_REFRESH, Map.of());
    }

    private String buildToken(String subject,
                              Instant issuedAt,
                              Duration validity,
                              String tokenType,
                              Map<String, Object> additionalClaims) {
        Instant expiration = issuedAt.plus(validity);
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .claims(additionalClaims)
                .claim(CLAIM_TYPE, tokenType)
                .signWith(secretKey)
                .compact();
    }

    public boolean validateAccessToken(String token) {
        return validateToken(token, TYPE_ACCESS);
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token, TYPE_REFRESH);
    }

    private boolean validateToken(String token, String expectedType) {
        Claims claims = parseClaims(token);
        String type = claims.get(CLAIM_TYPE, String.class);
        if (!expectedType.equals(type)) {
            throw new JwtException("Invalid token type");
        }
        return true;
    }

    public String getEmailFromAccessToken(String token) {
        return parseClaims(token).getSubject();
    }

    public String getEmailFromRefreshToken(String token) {
        return parseClaims(token).getSubject();
    }

    public List<String> getRoles(String token) {
        Claims claims = parseClaims(token);
        return claims.get(CLAIM_ROLES, List.class);
    }

    private Claims parseClaims(String token) {
        try {
            return jwtParser.parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException ex) {
            throw ex;
        } catch (JwtException ex) {
            throw ex;
        }
    }

    public Duration getAccessTokenValidity() {
        return accessTokenValidity;
    }

    public Duration getRefreshTokenValidity() {
        return refreshTokenValidity;
    }

    private List<String> extractAuthorities(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}
