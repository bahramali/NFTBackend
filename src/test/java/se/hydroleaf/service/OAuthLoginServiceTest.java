package se.hydroleaf.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import se.hydroleaf.config.OAuthProperties;
import se.hydroleaf.config.SecurityConfig;
import se.hydroleaf.model.OauthProvider;
import se.hydroleaf.model.User;
import se.hydroleaf.model.UserIdentity;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.model.UserStatus;
import se.hydroleaf.repository.UserIdentityRepository;
import se.hydroleaf.repository.UserRepository;

@DataJpaTest
@Import(SecurityConfig.class)
class OAuthLoginServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserIdentityRepository userIdentityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private OAuthLoginService service;
    private FakeTokenClient tokenClient;
    private FakeTokenVerifier tokenVerifier;

    @BeforeEach
    void setup() {
        Clock clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
        OAuthProperties properties = new OAuthProperties();
        properties.setFrontendBaseUrl("https://hydroleaf.se");
        properties.setAllowedRedirectUris(List.of("https://hydroleaf.se"));
        properties.getGoogle().setClientId("client-id");
        properties.getGoogle().setClientSecret("client-secret");
        properties.getGoogle().setRedirectUri("https://api.hydroleaf.se/api/auth/oauth/google/callback");

        OAuthStateStore stateStore = new OAuthStateStore(properties, clock);
        tokenClient = new FakeTokenClient();
        tokenVerifier = new FakeTokenVerifier();
        JwtService jwtService = Mockito.mock(JwtService.class);
        RefreshTokenService refreshTokenService = Mockito.mock(RefreshTokenService.class);
        Mockito.when(jwtService.createAccessToken(Mockito.any())).thenReturn("access-token");
        Mockito.when(refreshTokenService.createRefreshToken(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn("refresh-token");
        AuthService authService = new AuthService(userRepository, passwordEncoder, jwtService, refreshTokenService);
        service = new OAuthLoginService(
                properties,
                stateStore,
                tokenClient,
                tokenVerifier,
                authService,
                userRepository,
                userIdentityRepository,
                passwordEncoder
        );
    }

    @Test
    void callbackUsesExistingIdentity() {
        User user = userRepository.save(User.builder()
                .email("existing@example.com")
                .password(passwordEncoder.encode("secret"))
                .displayName("Existing")
                .role(UserRole.CUSTOMER)
                .permissions(Set.of())
                .active(true)
                .status(UserStatus.ACTIVE)
                .build());
        userIdentityRepository.save(UserIdentity.builder()
                .user(user)
                .provider(OauthProvider.GOOGLE)
                .providerSubject("sub-123")
                .providerEmail("existing@example.com")
                .build());

        tokenVerifier.claims = new OidcTokenClaims(
                "sub-123",
                "existing@example.com",
                true,
                "Existing",
                "https://example.com/pic.png"
        );

        OAuthLoginService.OAuthStartResult start = service.startLogin(OauthProvider.GOOGLE, "https://hydroleaf.se");
        OAuthLoginService.OAuthLoginResult result = service.handleCallback(
                OauthProvider.GOOGLE,
                "code",
                start.state()
        );

        assertThat(result.loginResult().user().userId()).isEqualTo(user.getId());
        assertThat(userIdentityRepository.findAll()).hasSize(1);
        assertThat(result.loginResult().accessToken()).isNotBlank();
    }

    @Test
    void callbackLinksByVerifiedEmail() {
        User user = userRepository.save(User.builder()
                .email("linked@example.com")
                .password(passwordEncoder.encode("secret"))
                .displayName("Linked")
                .role(UserRole.CUSTOMER)
                .permissions(Set.of())
                .active(true)
                .status(UserStatus.ACTIVE)
                .build());

        tokenVerifier.claims = new OidcTokenClaims(
                "sub-999",
                "linked@example.com",
                true,
                "Linked",
                null
        );

        OAuthLoginService.OAuthStartResult start = service.startLogin(OauthProvider.GOOGLE, "https://hydroleaf.se");
        OAuthLoginService.OAuthLoginResult result = service.handleCallback(
                OauthProvider.GOOGLE,
                "code",
                start.state()
        );

        UserIdentity identity = userIdentityRepository.findByProviderAndProviderSubject(
                OauthProvider.GOOGLE,
                "sub-999"
        ).orElseThrow();
        assertThat(identity.getUser().getId()).isEqualTo(user.getId());
        assertThat(result.loginResult().user().userId()).isEqualTo(user.getId());
    }

    @Test
    void callbackCreatesUserWhenNoMatch() {
        tokenVerifier.claims = new OidcTokenClaims(
                "sub-new",
                "new.user@example.com",
                true,
                "New User",
                "https://example.com/pic.png"
        );

        OAuthLoginService.OAuthStartResult start = service.startLogin(OauthProvider.GOOGLE, "https://hydroleaf.se");
        OAuthLoginService.OAuthLoginResult result = service.handleCallback(
                OauthProvider.GOOGLE,
                "code",
                start.state()
        );

        User saved = userRepository.findByEmailIgnoreCase("new.user@example.com").orElseThrow();
        assertThat(saved.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(saved.isEmailVerified()).isTrue();
        assertThat(result.loginResult().user().userId()).isEqualTo(saved.getId());
        assertThat(userIdentityRepository.findAll()).hasSize(1);
    }

    private static class FakeTokenClient implements OidcTokenClient {
        @Override
        public OidcTokenResponse exchangeAuthorizationCode(
                OauthProvider provider,
                String code,
                String codeVerifier,
                String redirectUri
        ) {
            return new OidcTokenResponse("access", "id-token", null, 3600L, "Bearer");
        }
    }

    private static class FakeTokenVerifier implements OidcTokenVerifier {
        private OidcTokenClaims claims;

        @Override
        public OidcTokenClaims verifyIdToken(OauthProvider provider, String idToken, String nonce) {
            return claims;
        }
    }
}
