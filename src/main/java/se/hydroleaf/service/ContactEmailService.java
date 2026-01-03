package se.hydroleaf.service;

import java.time.OffsetDateTime;
import se.hydroleaf.controller.dto.ContactRequest;

public interface ContactEmailService {
    void sendContactEmail(
            ContactRequest request,
            OffsetDateTime timestamp,
            String ip,
            String userAgent,
            String requestId
    );
}
