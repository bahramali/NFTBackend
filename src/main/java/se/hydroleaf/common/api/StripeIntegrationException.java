package se.hydroleaf.common.api;

import org.springframework.http.HttpStatus;

public class StripeIntegrationException extends ApiException {

    public StripeIntegrationException(String message) {
        super("STRIPE_ERROR", message, HttpStatus.BAD_GATEWAY);
    }
}
