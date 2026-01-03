package se.hydroleaf.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import se.hydroleaf.config.AuthProperties;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.UserRole;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String ROLE_CLAIM = "role";
    private static final String PERMISSIONS_CLAIM = "permissions";

    private final AuthProperties authProperties;
    private final Clock clock = Clock.systemUTC();

    public String createAccessToken(AuthenticatedUser user) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(authProperties.getJwt().getAccessTokenTtl());
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(String.valueOf(user.userId()))
                .issuer(authProperties.getJwt().getIssuer())
                .audience(authProperties.getJwt().getAudience())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiresAt))
                .claim(ROLE_CLAIM, user.role().name())
                .claim(PERMISSIONS_CLAIM, user.permissions().stream().map(Enum::name).toList())
                .build();
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            signedJWT.sign(new MACSigner(secret()));
            return signedJWT.serialize();
        } catch (JOSEException ex) {
            throw new IllegalStateException("Unable to sign access token", ex);
        }
    }

    public AuthenticatedUser parseAccessToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            if (!signedJWT.verify(new MACVerifier(secret()))) {
                throw new SecurityException("Invalid access token");
            }
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            validateClaims(claims);
            Long userId = Long.valueOf(claims.getSubject());
            UserRole role = UserRole.valueOf(requireClaim(claims, ROLE_CLAIM));
            List<String> permissionNames = claims.getStringListClaim(PERMISSIONS_CLAIM);
            Set<Permission> permissions = permissionNames == null
                    ? Set.of()
                    : permissionNames.stream().map(Permission::valueOf).collect(java.util.stream.Collectors.toSet());
            return new AuthenticatedUser(userId, role, permissions);
        } catch (ParseException | JOSEException | IllegalArgumentException ex) {
            throw new SecurityException("Invalid or expired access token", ex);
        }
    }

    private void validateClaims(JWTClaimsSet claims) {
        Instant now = Instant.now(clock);
        if (claims.getExpirationTime() == null || claims.getExpirationTime().toInstant().isBefore(now)) {
            throw new SecurityException("Access token expired");
        }
        if (!authProperties.getJwt().getIssuer().equals(claims.getIssuer())) {
            throw new SecurityException("Invalid access token issuer");
        }
        if (claims.getAudience() == null || !claims.getAudience().contains(authProperties.getJwt().getAudience())) {
            throw new SecurityException("Invalid access token audience");
        }
    }

    private String requireClaim(JWTClaimsSet claims, String name) throws ParseException {
        String value = claims.getStringClaim(name);
        if (value == null || value.isBlank()) {
            throw new SecurityException("Missing access token claim: " + name);
        }
        return value;
    }

    private byte[] secret() {
        String secret = authProperties.getJwt().getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is not configured");
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }
}
