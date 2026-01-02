package se.hydroleaf.service;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import se.hydroleaf.config.OAuthProperties;
import se.hydroleaf.model.OauthProvider;

@Service
@RequiredArgsConstructor
public class DefaultOidcTokenClient implements OidcTokenClient {

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

        try {
            OidcTokenResponse response = oauthWebClient.post()
                    .uri(google.getTokenEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .bodyToMono(OidcTokenResponse.class)
                    .onErrorResume(ex -> Mono.error(new ResponseStatusException(
                            HttpStatus.BAD_GATEWAY,
                            "Unable to exchange authorization code"
                    )))
                    .block();
            if (response == null || response.idToken() == null || response.idToken().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid token response");
            }
            return response;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to exchange authorization code");
        }
    }
}
