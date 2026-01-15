package se.hydroleaf.store.api.dto.orders.v1;

import java.time.Instant;
import java.util.UUID;
import se.hydroleaf.store.model.Payment;
import se.hydroleaf.store.model.StoreOrder;

/**
 * Order summary for list endpoints.
 *
 * <p>itemsCount represents the number of line items on the order. itemsQuantity is the sum of
 * quantities across all line items.</p>
 */
public record OrderSummaryDTO(
        UUID orderId,
        String orderNumber,
        Instant createdAt,
        Instant lastUpdatedAt,
        String orderStatus,
        String paymentStatus,
        String displayStatus,
        String currency,
        long totalCents,
        long totalAmountCents,
        int itemsCount,
        int itemsQuantity,
        String paymentProvider,
        String deliveryType,
        PaymentActionDTO paymentAction
) {

    public record ItemCounts(int itemsCount, int itemsQuantity) {
    }

    public static OrderSummaryDTO from(StoreOrder order, Payment payment, PaymentActionDTO paymentAction) {
        int count = order.getItems() != null ? order.getItems().size() : 0;
        int quantity = order.getItems() != null
                ? order.getItems().stream().mapToInt(item -> item.getQty()).sum()
                : 0;
        return from(order, payment, paymentAction, new ItemCounts(count, quantity));
    }

    public static OrderSummaryDTO from(StoreOrder order, Payment payment, PaymentActionDTO paymentAction,
                                      ItemCounts counts) {
        Instant lastUpdatedAt = payment != null ? payment.getUpdatedAt() : order.getCreatedAt();
        String paymentStatus = OrderStatusMapper.toPaymentStatus(payment);
        return new OrderSummaryDTO(
                order.getId(),
                order.getOrderNumber(),
                order.getCreatedAt(),
                lastUpdatedAt,
                order.getStatus().name(),
                paymentStatus,
                OrderStatusMapper.toDisplayStatus(order.getStatus(), paymentStatus),
                order.getCurrency(),
                order.getTotalCents(),
                order.getTotalAmountCents(),
                counts.itemsCount(),
                counts.itemsQuantity(),
                payment != null ? payment.getProvider().name() : null,
                deliveryType(order),
                paymentAction
        );
    }

    private static String deliveryType(StoreOrder order) {
        return order.getShippingAddress() != null ? "DELIVERY" : "PICKUP";
    }
}
