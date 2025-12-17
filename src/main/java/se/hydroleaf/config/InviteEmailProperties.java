package se.hydroleaf.config;

import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.invite-email")
public class InviteEmailProperties {

    private boolean smtpEnabled = false;
    private String from = "no-reply@hydroleaf.se";
    private String replyTo = "";
    private String subject = "You have been invited to Hydroleaf";
    private String inviteLinkTemplate = "";

    public boolean isSmtpEnabled() {
        return smtpEnabled;
    }

    public void setSmtpEnabled(boolean smtpEnabled) {
        this.smtpEnabled = smtpEnabled;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getInviteLinkTemplate() {
        return inviteLinkTemplate;
    }

    public void setInviteLinkTemplate(String inviteLinkTemplate) {
        this.inviteLinkTemplate = inviteLinkTemplate;
    }

    public Optional<String> renderInviteLink(String token) {
        if (inviteLinkTemplate == null || inviteLinkTemplate.isBlank()) {
            return Optional.empty();
        }
        String trimmed = inviteLinkTemplate.trim();
        if (trimmed.contains("{token}")) {
            return Optional.of(trimmed.replace("{token}", token));
        }
        if (trimmed.endsWith("/")) {
            return Optional.of(trimmed + token);
        }
        return Optional.of(trimmed + token);
    }
}
