package se.hydroleaf.service;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import se.hydroleaf.config.OAuthProperties;
import se.hydroleaf.model.OauthProvider;
import se.hydroleaf.model.User;
import se.hydroleaf.model.UserIdentity;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.model.UserStatus;
import se.hydroleaf.repository.UserIdentityRepository;
import se.hydroleaf.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class OAuthLoginService {

    private static final List<String> GOOGLE_SCOPES = List.of("openid", "email", "profile");

    private final OAuthProperties oauthProperties;
    private final OAuthStateStore stateStore;
    private final OidcTokenClient tokenClient;
    private final OidcTokenVerifier tokenVerifier;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public OAuthStartResult startLogin(OauthProvider provider, String redirectUri) {
        return startLogin(provider, redirectUri, null);
    }

    public OAuthStartResult startLogin(OauthProvider provider, String redirectUri, String callbackBaseUrl) {
        OAuthProperties.GoogleProperties google = requireGoogleConfig(provider);
        String resolvedRedirect = resolveRedirectUri(redirectUri);
        String callbackUri = resolveCallbackUri(google, callbackBaseUrl);
        String nonce = TokenGenerator.randomToken();
        String codeVerifier = TokenGenerator.randomVerifier();
        String codeChallenge = TokenGenerator.sha256Base64Url(codeVerifier);
        OAuthStateStore.OAuthState state = stateStore.createState(
                provider,
                nonce,
                codeVerifier,
                resolvedRedirect,
                callbackUri
        );

        String authorizationUrl = UriComponentsBuilder.fromUriString(google.getAuthorizationEndpoint())
                .queryParam("client_id", google.getClientId())
                .queryParam("redirect_uri", callbackUri)
                .queryParam("response_type", "code")
                .queryParam("scope", String.join(" ", GOOGLE_SCOPES))
                .queryParam("state", state.state())
                .queryParam("nonce", nonce)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .build()
                .toUriString();

        return new OAuthStartResult(authorizationUrl, state.state());
    }

    public OAuthLoginResult handleCallback(OauthProvider provider, String code, String state) {
        OAuthStateStore.OAuthState storedState = stateStore.consumeState(state)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OAuth state"));
        if (storedState.provider() != provider) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "OAuth provider mismatch");
        }

        OidcTokenResponse tokenResponse = tokenClient.exchangeAuthorizationCode(
                provider,
                code,
                storedState.codeVerifier(),
                storedState.callbackUri()
        );
        OidcTokenClaims claims = tokenVerifier.verifyIdToken(provider, tokenResponse.idToken(), storedState.nonce());
        if (claims == null || !StringUtils.hasText(claims.subject())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid ID token");
        }
        User user = upsertUser(provider, claims);
        AuthService.LoginResult loginResult = authService.createSession(user);
        return new OAuthLoginResult(loginResult, storedState.redirectUri());
    }

    private User upsertUser(OauthProvider provider, OidcTokenClaims claims) {
        Optional<UserIdentity> existingIdentity =
                userIdentityRepository.findByProviderAndProviderSubject(provider, claims.subject());
        if (existingIdentity.isPresent()) {
            UserIdentity identity = existingIdentity.get();
            if (StringUtils.hasText(claims.email())) {
                identity.setProviderEmail(claims.email());
            }
            userIdentityRepository.save(identity);
            return identity.getUser();
        }

        User linkedUser = findOrCreateUser(claims);
        UserIdentity identity = UserIdentity.builder()
                .user(linkedUser)
                .provider(provider)
                .providerSubject(claims.subject())
                .providerEmail(claims.email())
                .build();
        userIdentityRepository.save(identity);
        return linkedUser;
    }

    private User findOrCreateUser(OidcTokenClaims claims) {
        User user = null;
        if (claims.emailVerified() && StringUtils.hasText(claims.email())) {
            user = userRepository.findByEmailIgnoreCase(claims.email()).orElse(null);
        }

        if (user == null) {
            User created = User.builder()
                    .email(normalizeEmail(claims.email()))
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .displayName(resolveDisplayName(claims))
                    .role(UserRole.CUSTOMER)
                    .permissions(java.util.Set.of())
                    .active(true)
                    .status(UserStatus.ACTIVE)
                    .emailVerified(claims.emailVerified())
                    .pictureUrl(claims.pictureUrl())
                    .build();
            return userRepository.save(created);
        }

        if (claims.emailVerified() && !user.isEmailVerified()) {
            user.setEmailVerified(true);
        }
        if (StringUtils.hasText(claims.pictureUrl()) && !StringUtils.hasText(user.getPictureUrl())) {
            user.setPictureUrl(claims.pictureUrl());
        }
        if (StringUtils.hasText(claims.name()) && !StringUtils.hasText(user.getDisplayName())) {
            user.setDisplayName(claims.name());
        }
        return userRepository.save(user);
    }

    private String resolveDisplayName(OidcTokenClaims claims) {
        if (StringUtils.hasText(claims.name())) {
            return claims.name();
        }
        return claims.email();
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        return email.trim().toLowerCase();
    }

    private String resolveRedirectUri(String redirectUri) {
        if (!StringUtils.hasText(redirectUri)) {
            return oauthProperties.getFrontendBaseUrl();
        }
        List<String> allowed = oauthProperties.getAllowedRedirectUris();
        if (allowed == null || allowed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "OAuth redirect allowlist is not configured");
        }
        URI requested = URI.create(redirectUri);
        boolean allowedMatch = allowed.stream().filter(StringUtils::hasText).anyMatch(entry -> {
            URI allowedUri = URI.create(entry);
            boolean sameOrigin = Objects.equals(requested.getScheme(), allowedUri.getScheme())
                    && Objects.equals(requested.getHost(), allowedUri.getHost())
                    && normalizePort(requested) == normalizePort(allowedUri);
            String allowedPath = Optional.ofNullable(allowedUri.getPath()).orElse("/");
            return sameOrigin && requested.getPath().startsWith(allowedPath);
        });
        if (!allowedMatch) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Redirect URI not allowed");
        }
        return redirectUri;
    }

    private int normalizePort(URI uri) {
        if (uri.getPort() > 0) {
            return uri.getPort();
        }
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return 443;
        }
        if ("http".equalsIgnoreCase(uri.getScheme())) {
            return 80;
        }
        return -1;
    }

    private OAuthProperties.GoogleProperties requireGoogleConfig(OauthProvider provider) {
        if (provider != OauthProvider.GOOGLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported provider");
        }
        OAuthProperties.GoogleProperties google = oauthProperties.getGoogle();
        if (!StringUtils.hasText(google.getClientId())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Google OAuth client ID is not configured");
        }
        return google;
    }

    private String resolveCallbackUri(OAuthProperties.GoogleProperties google, String callbackBaseUrl) {
        if (StringUtils.hasText(google.getRedirectUri())) {
            return google.getRedirectUri();
        }
        if (!StringUtils.hasText(callbackBaseUrl)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Google OAuth redirect URI is not configured");
        }
        return UriComponentsBuilder.fromUriString(callbackBaseUrl)
                .path("/api/auth/oauth/google/callback")
                .build()
                .toUriString();
    }

    public record OAuthStartResult(String authorizationUrl, String state) {}

    public record OAuthLoginResult(AuthService.LoginResult loginResult, String redirectUri) {}
}
