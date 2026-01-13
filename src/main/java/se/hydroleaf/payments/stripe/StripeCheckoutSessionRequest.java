package se.hydroleaf.payments.stripe;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StripeCheckoutSessionRequest(
        @NotNull
        UUID orderId
) {
}
