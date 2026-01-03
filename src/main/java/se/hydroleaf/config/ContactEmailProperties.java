package se.hydroleaf.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.contact-email")
public class ContactEmailProperties {

    private boolean smtpEnabled = true;
    private String to = "info@hydroleaf.se";
    private String from = "no-reply@hydroleaf.se";
    private String subjectPrefix = "[HydroLeaf Contact]";

    public boolean isSmtpEnabled() {
        return smtpEnabled;
    }

    public void setSmtpEnabled(boolean smtpEnabled) {
        this.smtpEnabled = smtpEnabled;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getSubjectPrefix() {
        return subjectPrefix;
    }

    public void setSubjectPrefix(String subjectPrefix) {
        this.subjectPrefix = subjectPrefix;
    }
}
