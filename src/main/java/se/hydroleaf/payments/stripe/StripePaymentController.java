package se.hydroleaf.payments.stripe;

import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.common.api.BadRequestException;

@RestController
@RequestMapping("/api/payments/stripe")
public class StripePaymentController {

    private final StripePaymentService stripePaymentService;
    private final StripeCheckoutService stripeCheckoutService;

    public StripePaymentController(
            StripePaymentService stripePaymentService,
            StripeCheckoutService stripeCheckoutService
    ) {
        this.stripePaymentService = stripePaymentService;
        this.stripeCheckoutService = stripeCheckoutService;
    }

    @PostMapping("/payment-intents")
    public ResponseEntity<StripePaymentIntentResponse> createPaymentIntent(
            @Valid @RequestBody StripePaymentIntentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) throws StripeException {
        String resolvedIdempotencyKey = idempotencyKey;
        HttpHeaders headers = new HttpHeaders();
        if (!StringUtils.hasText(resolvedIdempotencyKey)) {
            if (StringUtils.hasText(request.orderId())) {
                resolvedIdempotencyKey = deterministicKey(request.orderId(), request.amount());
            } else {
                resolvedIdempotencyKey = UUID.randomUUID().toString();
            }
            headers.add("Idempotency-Key", resolvedIdempotencyKey);
        }

        StripePaymentService.StripePaymentIntentResult result = stripePaymentService
                .createPaymentIntent(request, resolvedIdempotencyKey);

        StripePaymentIntentResponse response = new StripePaymentIntentResponse(
                result.clientSecret(),
                result.paymentIntentId()
        );
        return ResponseEntity.ok()
                .headers(headers)
                .body(response);
    }

    @PostMapping("/checkout-session")
    public ResponseEntity<StripeCheckoutSessionResponse> createCheckoutSession(
            @Valid @RequestBody StripeCheckoutSessionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) throws StripeException {
        String resolvedIdempotencyKey = idempotencyKey;
        if (!StringUtils.hasText(resolvedIdempotencyKey)) {
            resolvedIdempotencyKey = request.orderId().toString();
        }

        StripeCheckoutSessionResponse response = stripeCheckoutService.createCheckoutSession(
                request.orderId(),
                resolvedIdempotencyKey
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "Stripe-Signature", required = false) String signature,
            @RequestBody byte[] payload
    ) {
        if (!StringUtils.hasText(signature)) {
            throw new BadRequestException("STRIPE_SIGNATURE_MISSING", "Stripe signature header is missing.");
        }
        stripeCheckoutService.handleWebhook(signature, payload);
        return ResponseEntity.ok().build();
    }

    private String deterministicKey(String orderId, Long amount) {
        String payload = orderId + ":" + amount;
        return UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
