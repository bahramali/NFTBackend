package se.hydroleaf.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import se.hydroleaf.config.InviteEmailProperties;

@ExtendWith(MockitoExtension.class)
class SmtpInviteEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Captor
    private ArgumentCaptor<SimpleMailMessage> messageCaptor;

    private InviteEmailProperties properties;

    @BeforeEach
    void setUp() {
        properties = new InviteEmailProperties();
        properties.setFrom("no-reply@example.com");
        properties.setSubject("Hydroleaf invite");
    }

    @Test
    void sendsEmailWithInviteLinkAndExpiry() {
        properties.setInviteLinkTemplate("https://example.com/accept?token={token}");
        SmtpInviteEmailService service = new SmtpInviteEmailService(mailSender, properties);

        LocalDateTime expiresAt = LocalDateTime.of(2024, 1, 2, 10, 15);
        service.sendInviteEmail("admin@example.com", "token-123", expiresAt);

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getTo()).containsExactly("admin@example.com");
        assertThat(message.getFrom()).isEqualTo("no-reply@example.com");
        assertThat(message.getSubject()).isEqualTo("Hydroleaf invite");
        assertThat(message.getText())
                .contains("https://example.com/accept?token=token-123")
                .contains("Invite token: token-123")
                .contains("Expires at: 2024-01-02T10:15:00");
    }

    @Test
    void sendsEmailWithTokenWhenNoLinkTemplateProvided() {
        properties.setInviteLinkTemplate("");
        SmtpInviteEmailService service = new SmtpInviteEmailService(mailSender, properties);

        service.sendInviteEmail("admin@example.com", "token-abc", null);

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getText())
                .contains("Invite token: token-abc")
                .doesNotContain("Accept your invite using this link:");
    }
}
