package se.hydroleaf.store.api.dto.orders.v1;

import java.time.Instant;
import java.util.UUID;
import se.hydroleaf.store.model.StoreOrder;

public record OrderSummaryDTO(
        UUID orderId,
        String orderNumber,
        Instant createdAt,
        String status,
        long totalCents,
        String currency,
        int itemsCount,
        int itemsQuantity
) {

    public static OrderSummaryDTO from(StoreOrder order) {
        int count = order.getItems() != null ? order.getItems().size() : 0;
        int quantity = order.getItems() != null
                ? order.getItems().stream().mapToInt(item -> item.getQty()).sum()
                : 0;
        return new OrderSummaryDTO(
                order.getId(),
                order.getOrderNumber(),
                order.getCreatedAt(),
                order.getStatus().name(),
                order.getTotalCents(),
                order.getCurrency(),
                count,
                quantity
        );
    }
}
