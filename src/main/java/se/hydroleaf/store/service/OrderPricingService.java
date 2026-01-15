package se.hydroleaf.store.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import se.hydroleaf.common.api.BadRequestException;
import se.hydroleaf.common.api.ConflictException;
import se.hydroleaf.common.api.NotFoundException;
import se.hydroleaf.store.model.OrderItem;
import se.hydroleaf.store.model.OrderStatus;
import se.hydroleaf.store.model.Payment;
import se.hydroleaf.store.model.PaymentStatus;
import se.hydroleaf.store.model.StoreOrder;
import se.hydroleaf.store.repository.OrderRepository;
import se.hydroleaf.store.repository.PaymentRepository;

@Service
@RequiredArgsConstructor
public class OrderPricingService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public long calculateTotal(UUID orderId) {
        StoreOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException("ORDER_CANCELED", "Order is canceled");
        }
        if (order.getStatus() != OrderStatus.OPEN) {
            throw new ConflictException("ORDER_NOT_PAYABLE", "Order is not payable");
        }

        Payment payment = paymentRepository.findTopByOrderIdOrderByUpdatedAtDesc(orderId).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.PAID) {
            throw new ConflictException("ORDER_ALREADY_PAID", "Order is already paid");
        }
        if (payment != null && payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new ConflictException("ORDER_REFUNDED", "Order has been refunded");
        }

        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new BadRequestException("ORDER_EMPTY", "Order has no items");
        }

        long total = 0L;
        for (OrderItem item : order.getItems()) {
            long lineTotal = Math.multiplyExact(item.getUnitPriceCents(), (long) item.getQty());
            total = Math.addExact(total, lineTotal);
        }

        if (total <= 0) {
            throw new BadRequestException("ORDER_TOTAL_INVALID", "Order total must be greater than zero");
        }

        return total;
    }
}
