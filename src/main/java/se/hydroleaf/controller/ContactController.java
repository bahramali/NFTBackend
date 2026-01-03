package se.hydroleaf.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.util.StringUtils;
import se.hydroleaf.controller.dto.ContactRequest;
import se.hydroleaf.service.ContactEmailService;
import se.hydroleaf.service.TurnstileVerificationService;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private static final Logger log = LoggerFactory.getLogger(ContactController.class);
    private static final Pattern URL_PATTERN = Pattern.compile("(?i)\\b((https?://|www\\.)\\S+)");
    private static final List<String> SUSPICIOUS_TERMS = List.of(
            "viagra",
            "casino",
            "porn",
            "adult",
            "loan",
            "forex",
            "bitcoin",
            "crypto",
            "telegram",
            "whatsapp",
            "seo",
            "backlink",
            "free money"
    );
    private static final List<String> SUSPICIOUS_DOMAINS = List.of(
            "bit.ly",
            "tinyurl.com",
            "t.co",
            "goo.gl",
            "ow.ly",
            "linktr.ee"
    );

    private final ContactEmailService contactEmailService;
    private final TurnstileVerificationService turnstileVerificationService;
    private final Clock clock;
    private final Environment environment;

    @PostMapping
    public ResponseEntity<Void> submitContact(
            @Valid @RequestBody ContactRequest request,
            HttpServletRequest httpRequest
    ) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        String ip = resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        ContactRequest sanitizedRequest = sanitizeRequest(request);
        String emailDomain = extractEmailDomain(sanitizedRequest.email());
        boolean honeypotTriggered = StringUtils.hasText(sanitizedRequest.companyWebsite());

        log.info(
                "contact_message_received requestId={} subject={} emailDomain={} ip={} userAgent={} honeypotTriggered={}",
                requestId,
                sanitizedRequest.subject(),
                emailDomain,
                ip,
                userAgent,
                honeypotTriggered
        );

        if (honeypotTriggered) {
            log.warn(
                    "contact_message_dropped requestId={} reason=honeypot ip={} userAgent={}",
                    requestId,
                    ip,
                    userAgent
            );
            return ResponseEntity.noContent().build();
        }

        if (!isDevProfile() && !StringUtils.hasText(sanitizedRequest.turnstileToken())) {
            log.warn(
                    "contact_message_dropped requestId={} reason=turnstile_missing ip={} userAgent={}",
                    requestId,
                    ip,
                    userAgent
            );
            return ResponseEntity.noContent().build();
        }

        if (StringUtils.hasText(sanitizedRequest.turnstileToken())) {
            TurnstileVerificationService.TurnstileVerificationResult verificationResult =
                    turnstileVerificationService.verify(sanitizedRequest.turnstileToken(), ip, requestId);
            if (!verificationResult.success()) {
                log.warn(
                        "contact_message_dropped requestId={} reason=turnstile_invalid ip={} userAgent={} errors={}",
                        requestId,
                        ip,
                        userAgent,
                        verificationResult.errors()
                );
                if (isDevProfile()) {
                    return ResponseEntity.badRequest().build();
                }
                return ResponseEntity.noContent().build();
            }
        }

        String contentIssue = validateMessageContent(sanitizedRequest.message());
        if (contentIssue != null) {
            log.warn(
                    "contact_message_dropped requestId={} reason=content_invalid issue={} ip={} userAgent={}",
                    requestId,
                    contentIssue,
                    ip,
                    userAgent
            );
            return ResponseEntity.badRequest().build();
        }

        OffsetDateTime timestamp = OffsetDateTime.now(clock);
        try {
            contactEmailService.sendContactEmail(sanitizedRequest, timestamp, ip, userAgent, requestId);
            log.info("contact_message_accepted requestId={} ip={} userAgent={}", requestId, ip, userAgent);
            return ResponseEntity.noContent().build();
        } catch (MailException ex) {
            log.error("Contact email failed requestId={} ip={} subject={}", requestId, ip, request.subject(), ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to send contact message");
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String remote = request.getRemoteAddr();
        return StringUtils.hasText(remote) ? remote : "unknown";
    }

    private String extractEmailDomain(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "unknown";
        }
        String[] parts = email.split("@", 2);
        return parts.length == 2 && StringUtils.hasText(parts[1]) ? parts[1].trim().toLowerCase() : "unknown";
    }

    private ContactRequest sanitizeRequest(ContactRequest request) {
        return new ContactRequest(
                sanitizeHeaderValue(request.fullName()),
                request.email(),
                request.phone(),
                request.subject(),
                request.message(),
                request.turnstileToken(),
                request.companyWebsite()
        );
    }

    private String sanitizeHeaderValue(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\r", " ").replace("\n", " ").trim();
    }

    private boolean isDevProfile() {
        return environment.acceptsProfiles(Profiles.of("dev"));
    }

    private String validateMessageContent(String message) {
        if (!StringUtils.hasText(message)) {
            return "message_empty";
        }
        int length = message.length();
        if (length < 20 || length > 2000) {
            return "message_length";
        }
        int urlCount = countUrls(message);
        if (urlCount > 2) {
            return "message_urls";
        }
        String lowered = message.toLowerCase();
        for (String term : SUSPICIOUS_TERMS) {
            if (lowered.contains(term)) {
                return "message_term";
            }
        }
        for (String domain : SUSPICIOUS_DOMAINS) {
            if (lowered.contains(domain)) {
                return "message_domain";
            }
        }
        return null;
    }

    private int countUrls(String message) {
        Matcher matcher = URL_PATTERN.matcher(message);
        int count = 0;
        while (matcher.find()) {
            count++;
            if (count > 2) {
                return count;
            }
        }
        return count;
    }
}
