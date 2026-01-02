package se.hydroleaf.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth")
@Getter
@Setter
public class OAuthProperties {

    private String frontendBaseUrl;

    private List<String> allowedRedirectUris = new ArrayList<>();

    private Duration stateTtl = Duration.ofMinutes(10);

    private RateLimitProperties rateLimit = new RateLimitProperties();

    private GoogleProperties google = new GoogleProperties();

    @Getter
    @Setter
    public static class RateLimitProperties {
        private long capacity = 60;
        private long refillTokens = 60;
        private long refillSeconds = 60;
    }

    @Getter
    @Setter
    public static class GoogleProperties {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String authorizationEndpoint = "https://accounts.google.com/o/oauth2/v2/auth";
        private String tokenEndpoint = "https://oauth2.googleapis.com/token";
        private String jwksUri = "https://www.googleapis.com/oauth2/v3/certs";
        private List<String> issuers = List.of("https://accounts.google.com", "accounts.google.com");
    }
}
