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
import se.hydroleaf.store.service.StripeService;
import se.hydroleaf.store.service.StripeWebhookOrderService;

@RestController
@RequestMapping("/api/store/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final StripeService stripeService;
    private final StripeWebhookOrderService stripeWebhookOrderService;

    @PostMapping("/stripe")
    public ResponseEntity<Void> handleStripe(@RequestBody String payload, @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        var session = stripeService.extractCompletedSession(payload, signature);
        if (session != null) {
            stripeWebhookOrderService.finalizePaidOrder(session);
            log.info("Stripe webhook processed for session {}", session.getId());
        } else {
            log.warn("Stripe webhook received without session id");
        }
        return ResponseEntity.ok().build();
    }
}
