package se.hydroleaf.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import se.hydroleaf.service.ContactEmailService;
import se.hydroleaf.service.SmtpContactEmailService;

@Configuration
@EnableConfigurationProperties(ContactEmailProperties.class)
@Slf4j
public class ContactEmailConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.contact-email", name = "smtp-enabled", havingValue = "true")
    public ContactEmailService smtpContactEmailService(
            JavaMailSender mailSender,
            ContactEmailProperties contactEmailProperties
    ) {
        log.info(
                "SMTP contact email service enabled with to={} from={} subjectPrefix={}",
                contactEmailProperties.getTo(),
                contactEmailProperties.getFrom(),
                contactEmailProperties.getSubjectPrefix());
        return new SmtpContactEmailService(mailSender, contactEmailProperties);
    }
}
