package se.hydroleaf.store.web;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.common.api.NotFoundException;
import se.hydroleaf.store.service.PaymentService;
import se.hydroleaf.store.service.StripeService;

@RestController
@RequestMapping("/api/store/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final StripeService stripeService;
    private final PaymentService paymentService;

    @PostMapping("/stripe")
    public ResponseEntity<Void> handleStripe(@RequestBody String payload, @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        String sessionId = stripeService.extractSessionId(payload, signature);
        if (sessionId != null) {
            try {
                paymentService.markPaid(sessionId);
                log.info("Stripe webhook processed for session {}", sessionId);
            } catch (NotFoundException ex) {
                log.warn("Stripe webhook received for unknown session {}", sessionId);
            }
        } else {
            log.warn("Stripe webhook received without session id");
        }
        return ResponseEntity.ok().build();
    }
}
