package se.hydroleaf.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.model.User;
import se.hydroleaf.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Duration RESET_COOLDOWN = Duration.ofMinutes(5);

    private final AuthorizationService authorizationService;
    private final UserRepository userRepository;
    private final PasswordResetEmailService passwordResetEmailService;
    private final Clock clock;
    private final Map<Long, Instant> lastResetRequests = new ConcurrentHashMap<>();

    public void requestPasswordReset(String token) {
        AuthenticatedUser authenticatedUser = authorizationService.requireAuthenticated(token);
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Instant now = clock.instant();
        Instant lastRequest = lastResetRequests.get(user.getId());
        if (lastRequest != null && lastRequest.plus(RESET_COOLDOWN).isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Password reset already requested");
        }
        lastResetRequests.put(user.getId(), now);
        String resetToken = UUID.randomUUID().toString();
        passwordResetEmailService.sendPasswordResetEmail(user.getEmail(), resetToken);
    }
}
