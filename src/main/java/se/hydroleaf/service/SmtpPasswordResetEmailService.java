package se.hydroleaf.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import se.hydroleaf.config.PasswordResetEmailProperties;

@Slf4j
public class SmtpPasswordResetEmailService implements PasswordResetEmailService {

    private final JavaMailSender mailSender;
    private final PasswordResetEmailProperties passwordResetEmailProperties;

    public SmtpPasswordResetEmailService(JavaMailSender mailSender, PasswordResetEmailProperties passwordResetEmailProperties) {
        this.mailSender = mailSender;
        this.passwordResetEmailProperties = passwordResetEmailProperties;
    }

    @Override
    public void sendPasswordResetEmail(String email, String token) {
        String toAddress = normalize(email);
        String replyToAddress = normalize(passwordResetEmailProperties.getReplyTo());
        String fromAddress = resolveFromAddress();
        if (toAddress.isBlank() || fromAddress.isBlank()) {
            throw new MailPreparationException("Password reset email requires valid to/from addresses");
        }
        String resetLink = requireResetLink(token);
        log.debug("Preparing to send password reset email via SMTP to {}", toAddress);
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(toAddress);
            helper.setFrom(fromAddress);
            if (!replyToAddress.isBlank()) {
                helper.setReplyTo(replyToAddress);
            }
            helper.setSubject(passwordResetEmailProperties.getSubject());
            helper.setText(buildBody(resetLink), false);
        } catch (MessagingException ex) {
            log.error("Failed to prepare password reset email to {} via SMTP: {}", toAddress, ex.getMessage(), ex);
            throw new MailPreparationException("Failed to prepare password reset email", ex);
        }

        log.debug("Password reset email payload for {} -> {}", toAddress, message);
        try {
            mailSender.send(message);
            log.info("Sent password reset email to {}", toAddress);
        } catch (MailException ex) {
            log.error("Failed to send password reset email to {} via SMTP: {}", toAddress, ex.getMessage(), ex);
            throw ex;
        }
    }

    private String buildBody(String resetLink) {
        StringBuilder body = new StringBuilder("A password reset was requested for your Hydroleaf account.");
        body.append("\n\nUse the link below to reset your password:\n");
        body.append(resetLink);
        body.append("\n\nIf you did not request this, you can ignore this email.");
        return body.toString();
    }

    private String requireResetLink(String token) {
        return passwordResetEmailProperties
                .renderResetLink(token)
                .map(link -> {
                    log.info("Generated password reset URL: {}", renderSafeLink());
                    return link;
                })
                .orElseThrow(() -> {
                    log.error(
                            "Password reset email reset link template is missing; configure app.password-reset-email.reset-link-template");
                    return new MailPreparationException("Password reset link template is missing");
                });
    }

    private String renderSafeLink() {
        return passwordResetEmailProperties.renderResetLink("<token>").orElse("<missing-template>");
    }

    private String resolveFromAddress() {
        String configured = normalize(passwordResetEmailProperties.getFrom());
        if (!configured.isBlank()) {
            return configured;
        }
        if (mailSender instanceof JavaMailSenderImpl impl) {
            String username = normalize(impl.getUsername());
            if (!username.isBlank()) {
                log.warn("Password reset email from address not set; falling back to SMTP username {}", username);
                return username;
            }
        }
        return "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
