package se.hydroleaf.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private JwtProperties jwt = new JwtProperties();
    private RefreshProperties refresh = new RefreshProperties();
    private CookieProperties cookie = new CookieProperties();

    @Getter
    @Setter
    public static class JwtProperties {
        private String secret;
        private String issuer;
        private String audience;
        private Duration accessTokenTtl = Duration.ofMinutes(15);
    }

    @Getter
    @Setter
    public static class RefreshProperties {
        private Duration tokenTtl = Duration.ofDays(30);
    }

    @Getter
    @Setter
    public static class CookieProperties {
        private String name = "refreshToken";
        private boolean secure = true;
        private boolean httpOnly = true;
        private String sameSite = "Lax";
        private String path = "/api/auth";
    }
}
