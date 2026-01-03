package se.hydroleaf.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.contact")
public class ContactProperties {

    private RateLimitProperties rateLimit = new RateLimitProperties();
    private TurnstileProperties turnstile = new TurnstileProperties();

    public RateLimitProperties getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimitProperties rateLimit) {
        this.rateLimit = rateLimit;
    }

    public TurnstileProperties getTurnstile() {
        return turnstile;
    }

    public void setTurnstile(TurnstileProperties turnstile) {
        this.turnstile = turnstile;
    }

    public static class RateLimitProperties {
        private LimitProperties perMinute = new LimitProperties(5, 5, 60);
        private LimitProperties perDay = new LimitProperties(20, 20, 86_400);

        public LimitProperties getPerMinute() {
            return perMinute;
        }

        public void setPerMinute(LimitProperties perMinute) {
            this.perMinute = perMinute;
        }

        public LimitProperties getPerDay() {
            return perDay;
        }

        public void setPerDay(LimitProperties perDay) {
            this.perDay = perDay;
        }
    }

    public static class LimitProperties {
        private long capacity;
        private long refillTokens;
        private long refillSeconds;

        public LimitProperties() {
        }

        public LimitProperties(long capacity, long refillTokens, long refillSeconds) {
            this.capacity = capacity;
            this.refillTokens = refillTokens;
            this.refillSeconds = refillSeconds;
        }

        public long getCapacity() {
            return capacity;
        }

        public void setCapacity(long capacity) {
            this.capacity = capacity;
        }

        public long getRefillTokens() {
            return refillTokens;
        }

        public void setRefillTokens(long refillTokens) {
            this.refillTokens = refillTokens;
        }

        public long getRefillSeconds() {
            return refillSeconds;
        }

        public void setRefillSeconds(long refillSeconds) {
            this.refillSeconds = refillSeconds;
        }
    }

    public static class TurnstileProperties {
        private String secret;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }
}
