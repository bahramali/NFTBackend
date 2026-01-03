package se.hydroleaf.service;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import se.hydroleaf.config.ContactProperties;

@Service
@RequiredArgsConstructor
@Slf4j
public class TurnstileVerificationService {

    private static final Duration TIMEOUT = Duration.ofSeconds(4);
    private static final String VERIFY_PATH = "/turnstile/v0/siteverify";

    private final ContactProperties contactProperties;
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://challenges.cloudflare.com")
            .build();

    public TurnstileVerificationResult verify(String token, String ip, String requestId) {
        String secret = contactProperties.getTurnstile() != null ? contactProperties.getTurnstile().getSecret() : null;
        if (!StringUtils.hasText(secret) || !StringUtils.hasText(token)) {
            log.warn(
                    "Turnstile verification skipped due to missing secret/token requestId={} hasSecret={} hasToken={}",
                    requestId,
                    StringUtils.hasText(secret),
                    StringUtils.hasText(token)
            );
            return TurnstileVerificationResult.invalid(List.of("missing-input"));
        }

        try {
            TurnstileResponse response = webClient.post()
                    .uri(VERIFY_PATH)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("secret", secret)
                            .with("response", token)
                            .with("remoteip", ip == null ? "" : ip))
                    .retrieve()
                    .bodyToMono(TurnstileResponse.class)
                    .block(TIMEOUT);

            if (response != null && response.success()) {
                return TurnstileVerificationResult.success();
            }
            List<String> errors = response != null ? response.errorCodes() : List.of("empty-response");
            log.warn(
                    "Turnstile verification failed requestId={} ip={} errors={}",
                    requestId,
                    ip,
                    errors
            );
            return TurnstileVerificationResult.invalid(errors);
        } catch (Exception ex) {
            log.error(
                    "Turnstile verification error requestId={} ip={} message={}",
                    requestId,
                    ip,
                    ex.getMessage()
            );
            return TurnstileVerificationResult.invalid(List.of("exception"));
        }
    }

    public record TurnstileVerificationResult(boolean success, List<String> errors) {
        public static TurnstileVerificationResult success() {
            return new TurnstileVerificationResult(true, List.of());
        }

        public static TurnstileVerificationResult invalid(List<String> errors) {
            return new TurnstileVerificationResult(false, errors == null ? List.of() : errors);
        }
    }

    public record TurnstileResponse(boolean success, List<String> errorCodes) {
    }
}
