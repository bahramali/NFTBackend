package se.hydroleaf.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import se.hydroleaf.config.OAuthProperties;
import se.hydroleaf.model.OauthProvider;

@Service
@RequiredArgsConstructor
public class OAuthStateStore {

    private final OAuthProperties oauthProperties;
    private final Clock clock;
    private final Map<String, OAuthState> states = new ConcurrentHashMap<>();

    public OAuthState createState(
            OauthProvider provider,
            String nonce,
            String codeVerifier,
            String redirectUri,
            String callbackUri
    ) {
        Instant now = Instant.now(clock);
        OAuthState state = new OAuthState(provider, nonce, codeVerifier, redirectUri, callbackUri, now);
        states.put(state.state(), state);
        return state;
    }

    public Optional<OAuthState> consumeState(String state) {
        OAuthState stored = states.remove(state);
        if (stored == null) {
            return Optional.empty();
        }
        Instant now = Instant.now(clock);
        Duration ttl = oauthProperties.getStateTtl();
        if (ttl != null && now.isAfter(stored.createdAt().plus(ttl))) {
            return Optional.empty();
        }
        return Optional.of(stored);
    }

    public record OAuthState(
            OauthProvider provider,
            String nonce,
            String codeVerifier,
            String redirectUri,
            String callbackUri,
            Instant createdAt,
            String state
    ) {
        public OAuthState(
                OauthProvider provider,
                String nonce,
                String codeVerifier,
                String redirectUri,
                String callbackUri,
                Instant createdAt
        ) {
            this(provider, nonce, codeVerifier, redirectUri, callbackUri, createdAt, TokenGenerator.randomToken());
        }
    }
}
