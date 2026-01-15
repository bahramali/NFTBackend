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
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.common.api.BadRequestException;
import se.hydroleaf.model.User;
import se.hydroleaf.repository.UserRepository;
import se.hydroleaf.service.AuthenticatedUser;
import se.hydroleaf.service.AuthorizationService;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/payments/stripe")
public class StripePaymentController {

    private final StripePaymentService stripePaymentService;
    private final StripeCheckoutService stripeCheckoutService;
    private final AuthorizationService authorizationService;
    private final UserRepository userRepository;

    public StripePaymentController(
            StripePaymentService stripePaymentService,
            StripeCheckoutService stripeCheckoutService,
            AuthorizationService authorizationService,
            UserRepository userRepository
    ) {
        this.stripePaymentService = stripePaymentService;
        this.stripeCheckoutService = stripeCheckoutService;
        this.authorizationService = authorizationService;
        this.userRepository = userRepository;
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
            @RequestHeader(name = "Authorization", required = false) String token,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) throws StripeException {
        String resolvedIdempotencyKey = idempotencyKey;
        if (!StringUtils.hasText(resolvedIdempotencyKey)) {
            resolvedIdempotencyKey = request.orderId().toString();
        }

        AuthenticatedUser authenticatedUser = authorizationService.requireAuthenticated(token);
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        StripeCheckoutSessionResponse response = stripeCheckoutService.createCheckoutSessionForUser(
                request.orderId(),
                resolvedIdempotencyKey,
                user.getEmail()
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
