package se.hydroleaf.store.service;

import jakarta.transaction.Transactional;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.hydroleaf.store.model.OrderStatus;
import se.hydroleaf.store.model.Payment;
import se.hydroleaf.store.model.PaymentStatus;
import se.hydroleaf.store.payment.PaymentProviderClient;
import se.hydroleaf.store.repository.OrderRepository;
import se.hydroleaf.store.repository.PaymentRepository;

@Service
@RequiredArgsConstructor
public class PaymentWebhookService {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookService.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public void handleWebhook(PaymentProviderClient client, Map<String, String> headers, byte[] rawBody) {
        if (!client.verifyWebhookSignature(headers, rawBody)) {
            throw new IllegalArgumentException("Invalid webhook signature");
        }

        PaymentProviderClient.WebhookEvent event = client.parseWebhookEvent(headers, rawBody);
        if (!StringUtils.hasText(event.providerPaymentId())) {
            log.warn("Webhook missing provider payment id");
            return;
        }

        Payment payment = paymentRepository.findByProviderPaymentId(event.providerPaymentId())
                .orElse(null);
        if (payment == null) {
            log.warn("Webhook received for unknown provider payment id {}", event.providerPaymentId());
            return;
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("Webhook already processed provider={} paymentId={} orderId={}",
                    payment.getProvider(),
                    event.providerPaymentId(),
                    payment.getOrder().getId());
            return;
        }

        PaymentStatus newStatus = mapStatus(event.status());
        payment.setStatus(newStatus);
        if (StringUtils.hasText(event.method())) {
            payment.setMethod(event.method());
        }
        if (event.amount() != null && event.amount() > 0) {
            if (payment.getAmountCents() != event.amount()) {
                log.warn("Webhook amount mismatch provider={} paymentId={} orderId={} expected={} received={}",
                        payment.getProvider(),
                        event.providerPaymentId(),
                        payment.getOrder().getId(),
                        payment.getAmountCents(),
                        event.amount());
            } else {
                payment.setAmountCents(event.amount());
            }
        }
        if (StringUtils.hasText(event.currency())) {
            payment.setCurrency(event.currency());
        }

        switch (newStatus) {
            case PAID -> payment.getOrder().setStatus(OrderStatus.PAID);
            case FAILED -> payment.getOrder().setStatus(OrderStatus.FAILED);
            case CANCELLED -> payment.getOrder().setStatus(OrderStatus.CANCELED);
            default -> {
            }
        }

        orderRepository.save(payment.getOrder());
        log.info("Processed webhook provider={} paymentId={} orderId={} status={}",
                payment.getProvider(),
                event.providerPaymentId(),
                payment.getOrder().getId(),
                newStatus);
    }

    private PaymentStatus mapStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return PaymentStatus.CREATED;
        }
        String normalized = status.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PAID", "CAPTURED", "CHARGED", "COMPLETED" -> PaymentStatus.PAID;
            case "FAILED" -> PaymentStatus.FAILED;
            case "CANCELED", "CANCELLED" -> PaymentStatus.CANCELLED;
            case "REFUNDED" -> PaymentStatus.REFUNDED;
            default -> PaymentStatus.CREATED;
        };
    }
}
