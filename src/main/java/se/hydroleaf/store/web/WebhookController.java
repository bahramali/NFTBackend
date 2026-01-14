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
        var webhookEvent = stripeService.extractWebhookEvent(payload, signature);
        if (webhookEvent == null) {
            log.warn("Stripe webhook received without event payload");
            return ResponseEntity.ok().build();
        }
        switch (webhookEvent.type()) {
            case "checkout.session.completed" -> {
                if (webhookEvent.session() != null) {
                    stripeWebhookOrderService.finalizePaidOrder(webhookEvent.session());
                    log.info("Stripe webhook processed for session {}", webhookEvent.session().getId());
                } else {
                    log.warn("Stripe webhook session completed without session payload");
                }
            }
            case "checkout.session.expired" -> {
                stripeWebhookOrderService.markCheckoutExpired(webhookEvent.session());
            }
            case "payment_intent.payment_failed" -> {
                stripeWebhookOrderService.markPaymentFailed(webhookEvent.paymentIntent());
            }
            default -> log.debug("Ignoring Stripe event type {}", webhookEvent.type());
        }
        return ResponseEntity.ok().build();
    }
}
