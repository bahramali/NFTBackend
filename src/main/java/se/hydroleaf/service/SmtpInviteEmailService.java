package se.hydroleaf.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import se.hydroleaf.config.InviteEmailProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class SmtpInviteEmailService implements InviteEmailService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final JavaMailSender mailSender;
    private final InviteEmailProperties inviteEmailProperties;

    @Override
    public void sendInviteEmail(String email, String token, LocalDateTime expiresAt) {
        String toAddress = normalize(email);
        String fromAddress = normalize(inviteEmailProperties.getFrom());
        String replyToAddress = normalize(inviteEmailProperties.getReplyTo());
        if (replyToAddress == null) {
            replyToAddress = fromAddress;
        }

        log.info(
                "Preparing to send admin invite email via SMTP to {} with from={} replyTo={} subject={} (inviteLinkTemplatePresent={})",
                toAddress,
                fromAddress,
                replyToAddress,
                inviteEmailProperties.getSubject(),
                inviteEmailProperties.getInviteLinkTemplate() != null
                        && !inviteEmailProperties.getInviteLinkTemplate().isBlank());

        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(toAddress);
            if (fromAddress != null) {
                helper.setFrom(fromAddress);
            }
            if (replyToAddress != null) {
                helper.setReplyTo(replyToAddress);
            }
            helper.setSubject(inviteEmailProperties.getSubject());
            helper.setText(buildBody(token, expiresAt), false);
            helper.setSentDate(new Date());
        } catch (MessagingException ex) {
            log.error("Failed to prepare admin invite email to {} via SMTP: {}", toAddress, ex.getMessage(), ex);
            throw new MailPreparationException("Failed to prepare admin invite email", ex);
        }

        log.debug("Admin invite email payload for {} -> {}", toAddress, message);

        try {
            mailSender.send(message);
            log.info("Sent admin invite email to {}", toAddress);
        } catch (MailException ex) {
            log.error("Failed to send admin invite email to {} via SMTP: {}", toAddress, ex.getMessage(), ex);
            throw ex;
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String buildBody(String token, LocalDateTime expiresAt) {
        StringBuilder body = new StringBuilder();
        body.append("You have been invited to the Hydroleaf admin console.\n\n");

        inviteEmailProperties
                .renderInviteLink(token)
                .ifPresent(link -> body.append("Accept your invite using this link: ")
                        .append(link)
                        .append("\n\n"));

        body.append("Invite token: ").append(token).append("\n");
        if (expiresAt != null) {
            body.append("Expires at: ")
                    .append(expiresAt.format(DATE_TIME_FORMATTER))
                    .append(" (server time)\n");
        }

        body.append("\nIf you did not expect this invitation, you can ignore this email.");
        return body.toString();
    }
}
