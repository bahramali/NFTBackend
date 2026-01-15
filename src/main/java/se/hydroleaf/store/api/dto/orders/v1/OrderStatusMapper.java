package se.hydroleaf.store.api.dto.orders.v1;

import se.hydroleaf.store.model.OrderStatus;
import se.hydroleaf.store.model.Payment;
import se.hydroleaf.store.model.PaymentStatus;

public final class OrderStatusMapper {

    private OrderStatusMapper() {
    }

    public static String toPaymentStatus(Payment payment) {
        if (payment == null) {
            return "PENDING";
        }
        return toPaymentStatus(payment.getStatus());
    }

    public static String toPaymentStatus(PaymentStatus status) {
        if (status == null) {
            return "PENDING";
        }
        return switch (status) {
            case PAID -> "SUCCEEDED";
            case FAILED, CANCELLED -> "FAILED";
            case REFUNDED -> "REFUNDED";
            case CREATED -> "PENDING";
        };
    }

    public static String toDisplayStatus(OrderStatus orderStatus, String paymentStatus) {
        if ("PENDING".equalsIgnoreCase(paymentStatus)) {
            return "Payment pending";
        }
        if ("FAILED".equalsIgnoreCase(paymentStatus)) {
            return "Payment failed";
        }
        if ("REFUNDED".equalsIgnoreCase(paymentStatus)) {
            return "Refunded";
        }
        return orderStatus == null ? null : switch (orderStatus) {
            case OPEN -> "Open";
            case PROCESSING -> "Processing";
            case SHIPPED -> "Shipped";
            case DELIVERED -> "Delivered";
            case CANCELLED -> "Cancelled";
        };
    }
}
