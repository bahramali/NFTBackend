package se.hydroleaf.store.api.dto.orders.v1;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import se.hydroleaf.store.model.OrderItem;
import se.hydroleaf.store.model.Payment;
import se.hydroleaf.store.model.PaymentStatus;
import se.hydroleaf.store.model.ShippingAddress;
import se.hydroleaf.store.model.StoreOrder;

public record OrderDetailsDTO(
        @JsonUnwrapped OrderSummaryDTO summary,
        String email,
        TotalsDTO totals,
        AddressDTO shippingAddress,
        AddressDTO billingAddress,
        PaymentInfoDTO payment,
        List<OrderItemDTO> items,
        String trackingUrl,
        List<OrderTimelineDTO> timeline
) {

    public static OrderDetailsDTO from(StoreOrder order, Payment payment) {
        return new OrderDetailsDTO(
                OrderSummaryDTO.from(order, payment),
                order.getEmail(),
                TotalsDTO.from(order),
                AddressDTO.from(order.getShippingAddress()),
                null,
                PaymentInfoDTO.from(payment),
                OrderItemDTO.from(order.getItems()),
                null,
                List.of()
        );
    }

    public record TotalsDTO(
            long subtotalCents,
            long shippingCents,
            long taxCents,
            Long discountCents,
            long totalCents
    ) {
        public static TotalsDTO from(StoreOrder order) {
            long discount = order.getTotalAmountCents() - order.getTotalCents();
            Long discountCents = discount > 0 ? discount : null;
            return new TotalsDTO(
                    order.getSubtotalCents(),
                    order.getShippingCents(),
                    order.getTaxCents(),
                    discountCents,
                    order.getTotalCents()
            );
        }
    }

    public record AddressDTO(
            String name,
            String street,
            String zip,
            String city,
            String country
    ) {
        public static AddressDTO from(ShippingAddress address) {
            if (address == null) {
                return null;
            }
            return new AddressDTO(
                    address.getName(),
                    formatStreet(address),
                    address.getPostalCode(),
                    address.getCity(),
                    address.getCountry()
            );
        }

        private static String formatStreet(ShippingAddress address) {
            if (address.getLine2() == null || address.getLine2().isBlank()) {
                return address.getLine1();
            }
            return address.getLine1() + " " + address.getLine2();
        }
    }

    public record PaymentInfoDTO(
            String provider,
            String status,
            String lastError,
            Instant paidAt
    ) {
        public static PaymentInfoDTO from(Payment payment) {
            if (payment == null) {
                return null;
            }
            Instant paidAt = payment.getStatus() == PaymentStatus.PAID ? payment.getUpdatedAt() : null;
            return new PaymentInfoDTO(
                    payment.getProvider().name(),
                    payment.getStatus().name(),
                    null,
                    paidAt
            );
        }
    }

    public record OrderItemDTO(
            UUID productId,
            String productName,
            UUID variantId,
            String variantName,
            int quantity,
            long unitPriceCents,
            long lineTotalCents
    ) {
        public static OrderItemDTO from(OrderItem item) {
            UUID productId = item.getProduct() != null ? item.getProduct().getId() : null;
            String productName = item.getProduct() != null ? item.getProduct().getName() : item.getNameSnapshot();
            UUID variantId = item.getVariant() != null ? item.getVariant().getId() : null;
            String variantName = item.getVariant() != null ? item.getVariant().getLabel() : null;
            return new OrderItemDTO(
                    productId,
                    productName,
                    variantId,
                    variantName,
                    item.getQty(),
                    item.getUnitPriceCents(),
                    item.getLineTotalCents()
            );
        }

        public static List<OrderItemDTO> from(List<OrderItem> items) {
            if (items == null || items.isEmpty()) {
                return List.of();
            }
            return items.stream().map(OrderItemDTO::from).toList();
        }
    }

    public record OrderTimelineDTO(
            String status,
            Instant timestamp
    ) {
    }
}
