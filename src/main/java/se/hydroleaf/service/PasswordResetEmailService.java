package se.hydroleaf.service;

import java.util.Optional;

public interface PasswordResetEmailService {
    void sendPasswordResetEmail(String email, String token);

    default Optional<String> lastTokenFor(String email) {
        return Optional.empty();
    }
}
