package se.hydroleaf.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private static final Logger log = LoggerFactory.getLogger(ContactController.class);

    private final ContactEmailService contactEmailService;
    private final Clock clock;

    @PostMapping
    public ResponseEntity<Void> submitContact(
            @Valid @RequestBody ContactRequest request,
            HttpServletRequest httpRequest
    ) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        String ip = resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        String emailDomain = extractEmailDomain(request.email());
        boolean honeypotTriggered = StringUtils.hasText(request.companyWebsite());

        log.info(
                "contact_message_received requestId={} subject={} emailDomain={} ip={} userAgent={} honeypotTriggered={}",
                requestId,
                request.subject(),
                emailDomain,
                ip,
                userAgent,
                honeypotTriggered
        );

        if (honeypotTriggered) {
            return ResponseEntity.ok().build();
        }

        OffsetDateTime timestamp = OffsetDateTime.now(clock);
        try {
            contactEmailService.sendContactEmail(request, timestamp, ip, userAgent, requestId);
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
}
