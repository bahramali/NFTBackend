package se.hydroleaf.payments.stripe;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stripe")
public class StripePaymentProperties {

    private String secretKey;
    private boolean allowClientAmount = false;

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public boolean isAllowClientAmount() {
        return allowClientAmount;
    }

    public void setAllowClientAmount(boolean allowClientAmount) {
        this.allowClientAmount = allowClientAmount;
    }
}
