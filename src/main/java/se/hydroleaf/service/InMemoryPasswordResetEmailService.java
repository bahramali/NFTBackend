package se.hydroleaf.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile({"dev", "test", "local"})
@ConditionalOnProperty(prefix = "app.password-reset-email", name = "smtp-enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryPasswordResetEmailService implements PasswordResetEmailService {

    private final Map<String, String> lastTokens = new ConcurrentHashMap<>();

    @Override
    public void sendPasswordResetEmail(String email, String token) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        log.info("Password reset email captured for {} (SMTP disabled)", normalized);
        lastTokens.put(normalized, token);
    }

    @Override
    public Optional<String> lastTokenFor(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(lastTokens.get(email.trim().toLowerCase()));
    }
}
