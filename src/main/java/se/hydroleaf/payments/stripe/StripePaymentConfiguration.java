package se.hydroleaf.payments.stripe;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(StripePaymentProperties.class)
public class StripePaymentConfiguration {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentConfiguration.class);

    private final StripePaymentProperties properties;

    public StripePaymentConfiguration(StripePaymentProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void initializeStripe() {
        if (!StringUtils.hasText(properties.getSecretKey())) {
            log.error("Missing STRIPE_SECRET_KEY; Stripe PaymentIntent integration cannot start.");
            throw new IllegalStateException("STRIPE_SECRET_KEY is required to initialize Stripe");
        }
        Stripe.apiKey = properties.getSecretKey();
        log.info("Stripe API key configured for PaymentIntent integration.");
    }
}
