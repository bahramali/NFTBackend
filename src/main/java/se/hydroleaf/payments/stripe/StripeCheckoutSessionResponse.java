package se.hydroleaf.payments.stripe;

public record StripeCheckoutSessionResponse(
        String sessionId,
        String url
) {
}
