package se.hydroleaf.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import se.hydroleaf.config.OAuthProperties;
import se.hydroleaf.model.OauthProvider;

@Service
@RequiredArgsConstructor
public class DefaultOidcTokenClient implements OidcTokenClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultOidcTokenClient.class);
    private static final Duration TOKEN_TIMEOUT = Duration.ofSeconds(15);

    private final WebClient oauthWebClient;
    private final OAuthProperties oauthProperties;

    @Override
    public OidcTokenResponse exchangeAuthorizationCode(
            OauthProvider provider,
            String code,
            String codeVerifier,
            String redirectUri
    ) {
        OAuthProperties.GoogleProperties google = oauthProperties.getGoogle();
        if (provider != OauthProvider.GOOGLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported provider");
        }
        if (google.getClientId() == null || google.getClientId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Google client ID is not configured");
        }
        if (google.getClientSecret() == null || google.getClientSecret().isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Google client secret is not configured");
        }
        String resolvedRedirectUri = (redirectUri != null && !redirectUri.isBlank())
                ? redirectUri
                : google.getRedirectUri();
        if (resolvedRedirectUri == null || resolvedRedirectUri.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Google redirect URI is not configured");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", Objects.requireNonNull(code));
        form.add("client_id", google.getClientId());
        form.add("client_secret", google.getClientSecret());
        form.add("redirect_uri", resolvedRedirectUri);
        if (codeVerifier != null) {
            form.add("code_verifier", codeVerifier);
        }

        Instant start = Instant.now();
        AtomicReference<HttpStatusCode> statusRef = new AtomicReference<>();
        log.info("OIDC token exchange start provider={} endpoint={}", provider, google.getTokenEndpoint());

        try {
            OidcTokenResponse response = oauthWebClient.post()
                    .uri(google.getTokenEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form))
                    .exchangeToMono(clientResponse -> {
                        statusRef.set(clientResponse.statusCode());
                        if (clientResponse.statusCode().isError()) {
                            return clientResponse.createException().flatMap(Mono::error);
                        }
                        return clientResponse.bodyToMono(OidcTokenResponse.class);
                    })
                    .timeout(TOKEN_TIMEOUT)
                    .block();
            log.info(
                    "OIDC token exchange complete provider={} status={} durationMs={}",
                    provider,
                    formatStatus(statusRef.get()),
                    Duration.between(start, Instant.now()).toMillis()
            );
            if (response == null || response.idToken() == null || response.idToken().isBlank()) {
                log.warn(
                        "OIDC token exchange invalid response provider={} status={} durationMs={}",
                        provider,
                        formatStatus(statusRef.get()),
                        Duration.between(start, Instant.now()).toMillis()
                );
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid token response");
            }
            return response;
        } catch (ResponseStatusException ex) {
            log.warn(
                    "OIDC token exchange failed provider={} status={} durationMs={} message={}",
                    provider,
                    formatStatus(statusRef.get()),
                    Duration.between(start, Instant.now()).toMillis(),
                    ex.getReason()
            );
            throw ex;
        } catch (Exception ex) {
            if (isTimeoutException(ex)) {
                log.warn(
                        "OIDC token exchange timeout provider={} durationMs={}",
                        provider,
                        Duration.between(start, Instant.now()).toMillis()
                );
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Google token endpoint timeout");
            }
            if (ex instanceof WebClientResponseException responseException) {
                statusRef.compareAndSet(null, responseException.getStatusCode());
            }
            log.warn(
                    "OIDC token exchange error provider={} status={} durationMs={} message={}",
                    provider,
                    formatStatus(statusRef.get()),
                    Duration.between(start, Instant.now()).toMillis(),
                    ex.getMessage()
            );
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to exchange authorization code");
        }
    }

    private boolean isTimeoutException(Throwable ex) {
        return ex instanceof TimeoutException || ex.getCause() instanceof TimeoutException;
    }

    private String formatStatus(HttpStatusCode status) {
        return status == null ? "unknown" : String.valueOf(status.value());
    }
}
