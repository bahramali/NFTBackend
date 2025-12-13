package se.hydroleaf.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setFrom(inviteEmailProperties.getFrom());
        message.setSubject(inviteEmailProperties.getSubject());
        message.setText(buildBody(token, expiresAt));
        mailSender.send(message);
        log.info("Sent admin invite email to {}", email);
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
