package se.hydroleaf.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class InMemoryInviteEmailService implements InviteEmailService {

    private final Map<String, String> lastTokens = new ConcurrentHashMap<>();

    @Override
    public void sendInviteEmail(String email, String token, LocalDateTime expiresAt) {
        log.info("Sending admin invite to {} expiring at {}", email, expiresAt);
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
