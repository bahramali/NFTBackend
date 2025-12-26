package se.hydroleaf.config;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import se.hydroleaf.service.PasswordResetEmailService;
import se.hydroleaf.service.SmtpPasswordResetEmailService;

@Configuration
@EnableConfigurationProperties(PasswordResetEmailProperties.class)
@Slf4j
public class PasswordResetEmailConfiguration {

    private final PasswordResetEmailProperties passwordResetEmailProperties;

    public PasswordResetEmailConfiguration(PasswordResetEmailProperties passwordResetEmailProperties) {
        this.passwordResetEmailProperties = passwordResetEmailProperties;
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.password-reset-email", name = "smtp-enabled", havingValue = "true")
    public PasswordResetEmailService smtpPasswordResetEmailService(
            JavaMailSender mailSender, PasswordResetEmailProperties passwordResetEmailProperties) {
        validateSmtpConfiguration(mailSender);
        log.info(
                "SMTP password reset email service enabled with from={} replyTo={} subject={} (resetLinkTemplatePresent={})",
                passwordResetEmailProperties.getFrom(),
                passwordResetEmailProperties.getReplyTo(),
                passwordResetEmailProperties.getSubject(),
                passwordResetEmailProperties.getResetLinkTemplate() != null
                        && !passwordResetEmailProperties.getResetLinkTemplate().isBlank());
        return new SmtpPasswordResetEmailService(mailSender, passwordResetEmailProperties);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logPasswordResetEmailMode() {
        log.info("Password reset SMTP is {}", passwordResetEmailProperties.isSmtpEnabled() ? "enabled" : "disabled");
    }

    private void validateSmtpConfiguration(JavaMailSender mailSender) {
        if (!(mailSender instanceof JavaMailSenderImpl impl)) {
            log.warn("Password reset SMTP is enabled, but JavaMailSender implementation cannot be validated");
            return;
        }
        List<String> missing = new ArrayList<>();
        if (isBlank(impl.getHost())) {
            missing.add("spring.mail.host");
        }
        if (impl.getPort() <= 0) {
            missing.add("spring.mail.port");
        }
        if (isBlank(impl.getUsername())) {
            missing.add("spring.mail.username");
        }
        if (isBlank(impl.getPassword())) {
            missing.add("spring.mail.password");
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Password reset SMTP is enabled but missing required mail configuration: " + String.join(", ", missing));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
