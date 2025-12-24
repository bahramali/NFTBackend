package se.hydroleaf.store.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import se.hydroleaf.common.api.BadRequestException;
import se.hydroleaf.common.api.ConflictException;
import se.hydroleaf.common.api.NotFoundException;
import se.hydroleaf.store.model.OrderItem;
import se.hydroleaf.store.model.OrderStatus;
import se.hydroleaf.store.model.StoreOrder;
import se.hydroleaf.store.repository.OrderRepository;

@Service
@RequiredArgsConstructor
public class OrderPricingService {

    private final OrderRepository orderRepository;

    public long calculateTotal(UUID orderId) {
        StoreOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ConflictException("ORDER_NOT_PENDING", "Order is not pending payment");
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
