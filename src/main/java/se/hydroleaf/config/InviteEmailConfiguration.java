package se.hydroleaf.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import se.hydroleaf.service.InviteEmailService;
import se.hydroleaf.service.SmtpInviteEmailService;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableConfigurationProperties(InviteEmailProperties.class)
@Slf4j
public class InviteEmailConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.invite-email", name = "smtp-enabled", havingValue = "true")
    public InviteEmailService smtpInviteEmailService(
            JavaMailSender mailSender, InviteEmailProperties inviteEmailProperties) {
        log.info(
                "SMTP invite email service enabled with from={} replyTo={} subject={} (inviteLinkTemplatePresent={})",
                inviteEmailProperties.getFrom(),
                inviteEmailProperties.getReplyTo(),
                inviteEmailProperties.getSubject(),
                inviteEmailProperties.getInviteLinkTemplate() != null
                        && !inviteEmailProperties.getInviteLinkTemplate().isBlank());
        return new SmtpInviteEmailService(mailSender, inviteEmailProperties);
    }

    @Bean
    public ApplicationListener<ApplicationReadyEvent> inviteEmailStartupLogger(
            Environment environment, InviteEmailService inviteEmailService) {
        return event -> {
            String smtpEnabled = environment.getProperty("app.invite-email.smtp-enabled");
            String mailHost = environment.getProperty("spring.mail.host");
            String mailPort = environment.getProperty("spring.mail.port");
            log.info(
                    "InviteEmailService active bean: {} (app.invite-email.smtp-enabled={}, spring.mail.host={}, spring.mail.port={})",
                    inviteEmailService.getClass().getSimpleName(),
                    smtpEnabled,
                    mailHost,
                    mailPort);
            log.info(
                    "Active Spring profiles: {}",
                    String.join(",", environment.getActiveProfiles()));
            log.info(
                    "SMTP env vars present: SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH={} SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE={}",
                    System.getenv().containsKey("SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH"),
                    System.getenv().containsKey("SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE"));
            log.info(
                    "Legacy SMTP env vars present (should be removed): SPRING_MAIL_SMTP_AUTH={} SPRING_MAIL_SMTP_STARTTLS_ENABLE={}",
                    System.getenv().containsKey("SPRING_MAIL_SMTP_AUTH"),
                    System.getenv().containsKey("SPRING_MAIL_SMTP_STARTTLS_ENABLE"));
            log.info(
                    "Invite email SMTP enabled flag present in env: APP_INVITE_EMAIL_SMTP_ENABLED={}",
                    System.getenv().containsKey("APP_INVITE_EMAIL_SMTP_ENABLED"));
        };
    }
}
