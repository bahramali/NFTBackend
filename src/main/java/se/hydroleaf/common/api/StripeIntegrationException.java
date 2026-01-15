package se.hydroleaf.common.api;

import com.stripe.exception.StripeException;
import org.springframework.http.HttpStatus;

public class StripeIntegrationException extends ApiException {

    public StripeIntegrationException(String code, String message, Throwable cause) {
        super(code, message, HttpStatus.BAD_GATEWAY);
        if (cause != null) {
            initCause(cause);
        }
    }

    public StripeIntegrationException(String code, String message) {
        this(code, message, null);
    }

    public static StripeIntegrationException fromStripeException(StripeException ex) {
        StripeErrorMapper.StripeError error = StripeErrorMapper.fromException(ex);
        return new StripeIntegrationException(error.code(), error.message(), ex);
    }
}
