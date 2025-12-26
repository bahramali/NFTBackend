package se.hydroleaf.config;

import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.password-reset-email")
public class PasswordResetEmailProperties {

    private boolean smtpEnabled = false;
    private String from = "no-reply@hydroleaf.se";
    private String replyTo = "";
    private String subject = "Reset your Hydroleaf password";
    private String resetLinkTemplate = "";
    private String publicBaseUrl = "";

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

    public String getResetLinkTemplate() {
        return resetLinkTemplate;
    }

    public void setResetLinkTemplate(String resetLinkTemplate) {
        this.resetLinkTemplate = resetLinkTemplate;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public Optional<String> renderResetLink(String token) {
        if (resetLinkTemplate == null || resetLinkTemplate.isBlank()) {
            String baseUrl = normalize(publicBaseUrl);
            if (baseUrl == null) {
                return Optional.empty();
            }
            return Optional.of(baseUrl + "/reset-password?token=" + token);
        }
        String trimmed = resetLinkTemplate.trim();
        if (trimmed.contains("{token}")) {
            return Optional.of(trimmed.replace("{token}", token));
        }
        if (trimmed.endsWith("/")) {
            return Optional.of(trimmed + token);
        }
        return Optional.of(trimmed + token);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
