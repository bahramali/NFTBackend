package se.hydroleaf.service;

import java.time.LocalDateTime;

public interface InviteEmailService {
    void sendInviteEmail(String email, String token, LocalDateTime expiresAt);

    default java.util.Optional<String> lastTokenFor(String email) {
        return java.util.Optional.empty();
    }
}
