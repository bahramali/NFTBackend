package se.hydroleaf.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import se.hydroleaf.service.InviteEmailService;
import se.hydroleaf.service.SmtpInviteEmailService;

@Configuration
@EnableConfigurationProperties(InviteEmailProperties.class)
public class InviteEmailConfiguration {

    @Bean
    @ConditionalOnBean(JavaMailSender.class)
    @ConditionalOnProperty(prefix = "app.invite-email", name = "smtp-enabled", havingValue = "true")
    public InviteEmailService smtpInviteEmailService(
            JavaMailSender mailSender, InviteEmailProperties inviteEmailProperties) {
        return new SmtpInviteEmailService(mailSender, inviteEmailProperties);
    }
}
