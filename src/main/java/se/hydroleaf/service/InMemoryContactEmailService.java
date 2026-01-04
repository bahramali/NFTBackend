package se.hydroleaf.service;

import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import se.hydroleaf.controller.dto.ContactRequest;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.contact-email", name = "smtp-enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryContactEmailService implements ContactEmailService {

    @Override
    public void sendContactEmail(
            ContactRequest request,
            OffsetDateTime timestamp,
            String ip,
            String userAgent,
            String requestId
    ) {
        log.info(
                "Contact email captured (SMTP disabled) requestId={} subject={} email={} ip={} timestamp={}",
                requestId,
                request.subject(),
                request.email(),
                ip,
                timestamp
        );
        log.info(
                "Contact auto-reply captured (SMTP disabled) requestId={} email={}",
                requestId,
                request.email()
        );
    }
}
