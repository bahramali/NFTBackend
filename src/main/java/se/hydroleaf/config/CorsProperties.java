package se.hydroleaf.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private static final List<String> DEFAULT_ALLOWED_ORIGINS = List.of(
            "https://hydroleaf.se",
            "https://www.hydroleaf.se",
            "https://app.hydroleaf.se",
            "http://localhost:5173"
    );

    private List<String> allowedOrigins = DEFAULT_ALLOWED_ORIGINS;

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            this.allowedOrigins = DEFAULT_ALLOWED_ORIGINS;
        } else {
            this.allowedOrigins = allowedOrigins;
        }
    }
}
