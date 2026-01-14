package se.hydroleaf.store.service;

import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import se.hydroleaf.common.api.NotFoundException;
import se.hydroleaf.store.config.NetsEasyProperties;
import se.hydroleaf.store.model.Payment;
import se.hydroleaf.store.model.PaymentProvider;
import se.hydroleaf.store.model.PaymentStatus;
import se.hydroleaf.store.model.StoreOrder;
import se.hydroleaf.store.payment.PaymentProviderClient;
import se.hydroleaf.store.repository.OrderRepository;
import se.hydroleaf.store.repository.PaymentRepository;

@Service
@RequiredArgsConstructor
public class CheckoutSessionService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderPricingService orderPricingService;
    private final PaymentProviderClient paymentProviderClient;
    private final NetsEasyProperties netsEasyProperties;

    @Transactional
    public CheckoutSessionResult createSession(UUID orderId) {
        StoreOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        long totalAmount = orderPricingService.calculateTotal(orderId);
        String currency = order.getCurrency();
        String successUrl = netsEasyProperties.getSuccessUrl().replace("{orderId}", orderId.toString());
        String cancelUrl = netsEasyProperties.getCancelUrl().replace("{orderId}", orderId.toString());

        order.setTotalCents(totalAmount);
        order.setTotalAmountCents(totalAmount);

        PaymentProviderClient.HostedCheckoutSession session = paymentProviderClient.createHostedCheckoutSession(
                order,
                totalAmount,
                currency,
                successUrl,
                cancelUrl
        );

        Payment payment = Payment.builder()
                .order(order)
                .provider(PaymentProvider.NETS_EASY)
                .status(PaymentStatus.CREATED)
                .amountCents(totalAmount)
                .currency(currency)
                .providerPaymentId(session.providerPaymentId())
                .providerReference(session.providerReference())
                .build();
        paymentRepository.save(payment);

        return new CheckoutSessionResult(session.redirectUrl(), payment.getId());
    }

    public record CheckoutSessionResult(String redirectUrl, UUID paymentId) {}
}
