package se.hydroleaf.store.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.store")
public class StoreProperties {

    private String currency = "SEK";
    private long shippingFlatCents = 0;
    private BigDecimal taxRatePercent = BigDecimal.ZERO;
    private String fallbackPaymentUrl = "https://hydroleaf.se/store/pay/{orderId}";
    private RateLimitProperties rateLimit = new RateLimitProperties();

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public long getShippingFlatCents() {
        return shippingFlatCents;
    }

    public void setShippingFlatCents(long shippingFlatCents) {
        this.shippingFlatCents = shippingFlatCents;
    }

    public BigDecimal getTaxRatePercent() {
        return taxRatePercent;
    }

    public void setTaxRatePercent(BigDecimal taxRatePercent) {
        this.taxRatePercent = taxRatePercent;
    }

    public String getFallbackPaymentUrl() {
        return fallbackPaymentUrl;
    }

    public void setFallbackPaymentUrl(String fallbackPaymentUrl) {
        this.fallbackPaymentUrl = fallbackPaymentUrl;
    }

    public RateLimitProperties getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimitProperties rateLimit) {
        this.rateLimit = rateLimit;
    }

    public static class RateLimitProperties {
        private long capacity = 120;
        private long refillTokens = 120;
        private long refillSeconds = 60;

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
}
