package se.hydroleaf.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jwt.proc.JWTProcessor;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.config.OAuthProperties;
import se.hydroleaf.model.OauthProvider;

@Service
@RequiredArgsConstructor
public class NimbusOidcTokenVerifier implements OidcTokenVerifier {

    private static final Duration DEFAULT_CLOCK_SKEW = Duration.ofSeconds(60);
    private final OAuthProperties oauthProperties;
    private final Clock clock;
    private final ConcurrentHashMap<OauthProvider, JWTProcessor<SecurityContext>> processors = new ConcurrentHashMap<>();

    @Override
    public OidcTokenClaims verifyIdToken(OauthProvider provider, String idToken, String nonce) {
        OAuthProperties.GoogleProperties google = oauthProperties.getGoogle();
        if (provider != OauthProvider.GOOGLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported provider");
        }
        if (google.getClientId() == null || google.getClientId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Google client ID is not configured");
        }

        JWTProcessor<SecurityContext> processor = processors.computeIfAbsent(provider, p -> buildProcessor(google));
        try {
            JWTClaimsSet claims = processor.process(idToken, null);
            validateClaims(provider, claims, nonce, google);
            String email = claims.getStringClaim("email");
            Boolean emailVerified = claims.getBooleanClaim("email_verified");
            String name = claims.getStringClaim("name");
            String picture = claims.getStringClaim("picture");
            return new OidcTokenClaims(
                    claims.getSubject(),
                    email,
                    emailVerified != null && emailVerified,
                    name,
                    picture
            );
        } catch (BadJOSEException | JOSEException | ParseException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid ID token");
        }
    }

    private JWTProcessor<SecurityContext> buildProcessor(OAuthProperties.GoogleProperties google) {
        try {
            URL jwksUrl = new URL(google.getJwksUri());
            JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(jwksUrl);
            JWSKeySelector<SecurityContext> selector =
                    new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);
            DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
            processor.setJWSKeySelector(selector);
            return processor;
        } catch (MalformedURLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid JWKS configuration");
        }
    }

    private void validateClaims(
            OauthProvider provider,
            JWTClaimsSet claims,
            String nonce,
            OAuthProperties.GoogleProperties google
    ) throws BadJOSEException {
        if (claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw new BadJOSEException("Missing subject");
        }
        List<String> issuers = google.getIssuers();
        if (issuers != null && !issuers.isEmpty() && !issuers.contains(claims.getIssuer())) {
            throw new BadJOSEException("Invalid issuer");
        }
        if (claims.getAudience() == null || !claims.getAudience().contains(google.getClientId())) {
            throw new BadJOSEException("Invalid audience");
        }
        Date expiration = claims.getExpirationTime();
        if (expiration == null) {
            throw new BadJOSEException("Missing expiration");
        }
        Instant now = Instant.now(clock);
        if (now.isAfter(expiration.toInstant().plus(DEFAULT_CLOCK_SKEW))) {
            throw new BadJOSEException("Token expired");
        }
        try {
            String tokenNonce = claims.getStringClaim("nonce");
            if (nonce != null && !nonce.equals(tokenNonce)) {
                throw new BadJOSEException("Invalid nonce");
            }
        } catch (ParseException ex) {
            throw new BadJOSEException("Invalid nonce");
        }
    }
}
