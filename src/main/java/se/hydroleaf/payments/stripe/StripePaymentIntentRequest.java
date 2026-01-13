package se.hydroleaf.payments.stripe;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StripePaymentIntentRequest(
        @NotNull
        @Positive
        Long amount,
        String currency,
        String orderId,
        @Email
        String customerEmail,
        String description
) {
}
