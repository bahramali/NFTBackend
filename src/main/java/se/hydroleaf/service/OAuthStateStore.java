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
    private final Map<String, Instant> consumedStates = new ConcurrentHashMap<>();

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

    public Optional<OAuthState> getState(String state) {
        OAuthState stored = states.get(state);
        if (stored == null) {
            return Optional.empty();
        }
        Instant now = Instant.now(clock);
        Duration ttl = oauthProperties.getStateTtl();
        if (ttl != null && now.isAfter(stored.createdAt().plus(ttl))) {
            states.remove(state);
            return Optional.empty();
        }
        return Optional.of(stored);
    }

    public void removeState(String state) {
        OAuthState removed = states.remove(state);
        if (removed != null) {
            consumedStates.put(state, Instant.now(clock));
        }
    }

    public Optional<OAuthState> consumeState(String state) {
        Optional<OAuthState> stored = getState(state);
        stored.ifPresent(value -> removeState(state));
        return stored;
    }

    public boolean wasConsumed(String state) {
        Instant consumedAt = consumedStates.get(state);
        if (consumedAt == null) {
            return false;
        }
        Duration ttl = oauthProperties.getStateTtl();
        if (ttl != null && Instant.now(clock).isAfter(consumedAt.plus(ttl))) {
            consumedStates.remove(state);
            return false;
        }
        return true;
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
