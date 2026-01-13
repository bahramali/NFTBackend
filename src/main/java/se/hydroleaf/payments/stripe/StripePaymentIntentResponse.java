package se.hydroleaf.payments.stripe;

public record StripePaymentIntentResponse(
        String clientSecret,
        String paymentIntentId
) {
}
