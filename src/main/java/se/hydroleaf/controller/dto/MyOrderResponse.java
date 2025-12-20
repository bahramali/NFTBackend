package se.hydroleaf.controller.dto;

import java.time.Instant;
import java.util.UUID;
import se.hydroleaf.store.model.StoreOrder;

public record MyOrderResponse(
        UUID id,
        String orderNumber,
        String status,
        long totalCents,
        String currency,
        Instant createdAt
) {

    public static MyOrderResponse from(StoreOrder order) {
        return new MyOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus().name(),
                order.getTotalCents(),
                order.getCurrency(),
                order.getCreatedAt()
        );
    }
}
