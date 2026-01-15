package se.hydroleaf.common.api;

import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.RateLimitException;
import com.stripe.exception.StripeException;

public final class StripeErrorMapper {

    private StripeErrorMapper() {
    }

    public static StripeError fromException(StripeException ex) {
        if (ex instanceof RateLimitException) {
            return new StripeError("STRIPE_RATE_LIMITED", "Stripe is temporarily rate limited. Please retry.");
        }
        if (ex instanceof AuthenticationException) {
            return new StripeError("STRIPE_AUTH_FAILED", "Stripe authentication failed.");
        }
        if (ex instanceof InvalidRequestException) {
            return new StripeError("STRIPE_INVALID_REQUEST", "Stripe request was invalid.");
        }
        if (ex instanceof APIConnectionException) {
            return new StripeError("STRIPE_CONNECTION_ERROR", "Unable to reach Stripe. Please retry.");
        }
        if (ex instanceof APIException) {
            return new StripeError("STRIPE_API_ERROR", "Stripe service error. Please retry.");
        }
        if (ex instanceof CardException) {
            return new StripeError("STRIPE_CARD_DECLINED", "Payment method was declined.");
        }
        return new StripeError("STRIPE_ERROR", "Stripe request failed.");
    }

    public record StripeError(String code, String message) {
    }
}
