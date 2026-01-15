package se.hydroleaf.store.api.dto.orders.v1;

import java.time.Instant;
import java.util.UUID;
import se.hydroleaf.store.model.OrderStatus;
import se.hydroleaf.store.model.Payment;
import se.hydroleaf.store.model.StoreOrder;

public record OrderSummaryDTO(
        UUID orderId,
        String orderNumber,
        Instant createdAt,
        Instant lastUpdatedAt,
        String status,
        String statusDisplay,
        String currency,
        long totalCents,
        long totalAmountCents,
        int itemsCount,
        int itemsQuantity,
        String paymentProvider,
        String paymentStatus,
        String deliveryType
) {

    public static OrderSummaryDTO from(StoreOrder order, Payment payment) {
        int count = order.getItems() != null ? order.getItems().size() : 0;
        int quantity = order.getItems() != null
                ? order.getItems().stream().mapToInt(item -> item.getQty()).sum()
                : 0;
        Instant lastUpdatedAt = payment != null ? payment.getUpdatedAt() : order.getCreatedAt();
        return new OrderSummaryDTO(
                order.getId(),
                order.getOrderNumber(),
                order.getCreatedAt(),
                lastUpdatedAt,
                order.getStatus().name(),
                statusDisplay(order.getStatus()),
                order.getCurrency(),
                order.getTotalCents(),
                order.getTotalAmountCents(),
                count,
                quantity,
                payment != null ? payment.getProvider().name() : null,
                payment != null ? payment.getStatus().name() : null,
                deliveryType(order)
        );
    }

    private static String statusDisplay(OrderStatus status) {
        return switch (status) {
            case PENDING_PAYMENT -> "Pending payment";
            case PAID -> "Paid";
            case FAILED -> "Failed";
            case CANCELED -> "Canceled";
        };
    }

    private static String deliveryType(StoreOrder order) {
        return order.getShippingAddress() != null ? "DELIVERY" : "PICKUP";
    }
}
