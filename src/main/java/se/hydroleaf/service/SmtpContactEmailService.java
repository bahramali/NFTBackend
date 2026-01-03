package se.hydroleaf.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.util.HtmlUtils;
import se.hydroleaf.config.ContactEmailProperties;
import se.hydroleaf.controller.dto.ContactRequest;

@RequiredArgsConstructor
@Slf4j
public class SmtpContactEmailService implements ContactEmailService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final String FIXED_TO = "info@hydroleaf.se";
    private static final String FIXED_FROM = "no-reply@hydroleaf.se";

    private final JavaMailSender mailSender;
    private final ContactEmailProperties contactEmailProperties;

    @Override
    public void sendContactEmail(
            ContactRequest request,
            OffsetDateTime timestamp,
            String ip,
            String userAgent,
            String requestId
    ) {
        String toAddress = normalize(FIXED_TO);
        String fromAddress = resolveFromAddress();
        String replyToAddress = normalize(sanitizeHeaderValue(request.email()));
        if (toAddress == null || fromAddress == null || replyToAddress == null) {
            throw new MailPreparationException("Contact email requires valid to/from/replyTo addresses");
        }

        String subject = contactEmailProperties.getSubjectPrefix()
                + " " + sanitizeHeaderValue(request.subject().toString())
                + " - " + sanitizeHeaderValue(request.fullName());

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(toAddress);
            helper.setFrom(fromAddress);
            helper.setReplyTo(replyToAddress);
            helper.setSubject(subject);
            helper.setText(buildBody(request, timestamp, ip, userAgent, requestId), false);
            helper.setSentDate(new Date());
        } catch (MessagingException ex) {
            log.error("Failed to prepare contact email requestId={} to={} error={}", requestId, toAddress, ex.getMessage(), ex);
            throw new MailPreparationException("Failed to prepare contact email", ex);
        }

        try {
            mailSender.send(message);
            log.info("Contact email sent requestId={} to={}", requestId, toAddress);
        } catch (MailException ex) {
            log.error("SMTP send failed for contact requestId={} to={} error={}", requestId, toAddress, ex.getMessage());
            if (mailSender instanceof JavaMailSenderImpl impl) {
                log.error(
                        "SMTP transport details host={} port={} username={} auth={} starttls={}",
                        impl.getHost(),
                        impl.getPort(),
                        impl.getUsername(),
                        impl.getJavaMailProperties().getProperty("mail.smtp.auth"),
                        impl.getJavaMailProperties().getProperty("mail.smtp.starttls.enable"));
            }
            log.error("Failed to send contact email requestId={} to={} error={}", requestId, toAddress, ex.getMessage(), ex);
            throw ex;
        }
    }

    private String resolveFromAddress() {
        return normalize(FIXED_FROM);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String buildBody(
            ContactRequest request,
            OffsetDateTime timestamp,
            String ip,
            String userAgent,
            String requestId
    ) {
        StringBuilder body = new StringBuilder();
        body.append("HydroLeaf contact request received.\n\n");
        body.append("Request ID: ").append(requestId).append("\n");
        body.append("Full name: ").append(escapePlainText(request.fullName())).append("\n");
        body.append("Email: ").append(escapePlainText(request.email())).append("\n");
        if (request.phone() != null && !request.phone().isBlank()) {
            body.append("Phone: ").append(escapePlainText(request.phone().trim())).append("\n");
        }
        body.append("Subject: ").append(escapePlainText(request.subject().toString())).append("\n");
        body.append("Message:\n").append(escapePlainText(request.message())).append("\n\n");
        body.append("Timestamp: ").append(timestamp.format(DATE_TIME_FORMATTER)).append("\n");
        body.append("IP: ").append(escapePlainText(ip == null ? "unknown" : ip)).append("\n");
        if (userAgent != null && !userAgent.isBlank()) {
            body.append("User-Agent: ").append(escapePlainText(userAgent)).append("\n");
        }
        return body.toString();
    }

    private String sanitizeHeaderValue(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\r", " ").replace("\n", " ").trim();
    }

    private String escapePlainText(String value) {
        if (value == null) {
            return "";
        }
        String sanitized = value.replace("\r", "");
        return HtmlUtils.htmlEscape(sanitized);
    }
}
