package se.hydroleaf.store.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.hydroleaf.common.api.NotFoundException;
import se.hydroleaf.store.model.OrderStatus;
import se.hydroleaf.store.model.Payment;
import se.hydroleaf.store.model.PaymentStatus;
import se.hydroleaf.store.model.StoreOrder;
import se.hydroleaf.store.repository.OrderRepository;
import se.hydroleaf.store.repository.PaymentRepository;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public void markPaid(String providerRef) {
        Payment payment = paymentRepository.findByProviderPaymentId(providerRef)
                .orElseThrow(() -> new NotFoundException("PAYMENT_NOT_FOUND", "Payment not found"));

        if (payment.getStatus() == PaymentStatus.PAID) {
            return;
        }

        payment.setStatus(PaymentStatus.PAID);
        StoreOrder order = payment.getOrder();
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
        log.info("Marked order as paid orderId={} providerRef={}", order.getId(), providerRef);
    }
}
