package se.hydroleaf.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import se.hydroleaf.config.InviteEmailProperties;

@ExtendWith(MockitoExtension.class)
class SmtpInviteEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Captor
    private ArgumentCaptor<MimeMessage> messageCaptor;

    private InviteEmailProperties properties;

    @BeforeEach
    void setUp() {
        properties = new InviteEmailProperties();
        properties.setFrom("no-reply@example.com");
        properties.setSubject("Hydroleaf invite");
    }

    @Test
    void sendsEmailWithInviteLinkAndExpiry() throws Exception {
        properties.setInviteLinkTemplate("https://example.com/accept?token={token}");
        SmtpInviteEmailService service = new SmtpInviteEmailService(mailSender, properties);

        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        LocalDateTime expiresAt = LocalDateTime.of(2024, 1, 2, 10, 15);
        service.sendInviteEmail("admin@example.com", "token-123", expiresAt);

        verify(mailSender).send(messageCaptor.capture());
        MimeMessage message = messageCaptor.getValue();
        assertThat(message.getAllRecipients()).extracting(Object::toString).containsExactly("admin@example.com");
        assertThat(message.getFrom()).extracting(Object::toString).containsExactly("no-reply@example.com");
        assertThat(message.getReplyTo()).extracting(Object::toString).containsExactly("no-reply@example.com");
        assertThat(message.getSubject()).isEqualTo("Hydroleaf invite");
        assertThat(message.getContent().toString())
                .contains("https://example.com/accept?token=token-123")
                .contains("Invite token: token-123")
                .contains("Expires at: 2024-01-02T10:15:00");
    }

    @Test
    void sendsEmailWithTokenWhenNoLinkTemplateProvided() throws Exception {
        properties.setInviteLinkTemplate("");
        properties.setReplyTo("support@example.com");
        SmtpInviteEmailService service = new SmtpInviteEmailService(mailSender, properties);

        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        service.sendInviteEmail("admin@example.com", "token-abc", null);

        verify(mailSender).send(messageCaptor.capture());
        MimeMessage message = messageCaptor.getValue();
        assertThat(message.getReplyTo()).extracting(Object::toString).containsExactly("support@example.com");
        assertThat(message.getContent().toString())
                .contains("Invite token: token-abc")
                .doesNotContain("Accept your invite using this link:");
    }

    @Test
    void fallsBackToSmtpUsernameWhenFromMissing() throws Exception {
        properties.setFrom("  ");
        properties.setReplyTo("");
        StubJavaMailSender mailSender = new StubJavaMailSender();
        mailSender.setUsername("no-reply@example.com");

        SmtpInviteEmailService service = new SmtpInviteEmailService(mailSender, properties);

        service.sendInviteEmail("admin@example.com", "token-fallback", null);

        MimeMessage message = mailSender.lastMessage;
        assertThat(message.getFrom()).extracting(Object::toString).containsExactly("no-reply@example.com");
        assertThat(message.getReplyTo()).extracting(Object::toString).containsExactly("no-reply@example.com");
    }

    private static final class StubJavaMailSender extends JavaMailSenderImpl {
        private MimeMessage lastMessage;

        @Override
        protected void doSend(MimeMessage[] mimeMessages, Object[] originalMessages) {
            this.lastMessage = mimeMessages[0];
        }
    }
}
