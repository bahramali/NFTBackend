package se.hydroleaf.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import se.hydroleaf.service.PasswordResetEmailService;
import se.hydroleaf.service.SmtpPasswordResetEmailService;

@Configuration
@EnableConfigurationProperties(PasswordResetEmailProperties.class)
@Slf4j
public class PasswordResetEmailConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.password-reset-email", name = "smtp-enabled", havingValue = "true")
    public PasswordResetEmailService smtpPasswordResetEmailService(
            JavaMailSender mailSender, PasswordResetEmailProperties passwordResetEmailProperties) {
        log.info(
                "SMTP password reset email service enabled with from={} replyTo={} subject={} (resetLinkTemplatePresent={})",
                passwordResetEmailProperties.getFrom(),
                passwordResetEmailProperties.getReplyTo(),
                passwordResetEmailProperties.getSubject(),
                passwordResetEmailProperties.getResetLinkTemplate() != null
                        && !passwordResetEmailProperties.getResetLinkTemplate().isBlank());
        return new SmtpPasswordResetEmailService(mailSender, passwordResetEmailProperties);
    }
}
