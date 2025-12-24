package se.hydroleaf.store.web;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.store.payment.NetsEasyClient;
import se.hydroleaf.store.service.PaymentWebhookService;

@RestController
@RequestMapping("/api/payments/webhook")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final PaymentWebhookService paymentWebhookService;
    private final NetsEasyClient netsEasyClient;

    @PostMapping("/nets")
    public ResponseEntity<Void> handleNetsWebhook(@RequestHeader Map<String, String> headers, @RequestBody byte[] payload) {
        try {
            paymentWebhookService.handleWebhook(netsEasyClient, headers, payload);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException ex) {
            log.warn("Nets webhook rejected: {}", ex.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
