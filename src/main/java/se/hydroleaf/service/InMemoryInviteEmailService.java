package se.hydroleaf.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.invite-email", name = "smtp-enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryInviteEmailService implements InviteEmailService {

    private final Map<String, String> lastTokens = new ConcurrentHashMap<>();

    @Override
    public void sendInviteEmail(String email, String token, LocalDateTime expiresAt) {
        log.info("Invite email captured for {} expiring at {} (SMTP disabled)", email, expiresAt);
        lastTokens.put(email.trim().toLowerCase(), token);
    }

    @Override
    public Optional<String> lastTokenFor(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(lastTokens.get(email.trim().toLowerCase()));
    }
}
