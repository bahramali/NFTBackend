package se.hydroleaf.store.api.dto.orders.v1;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import se.hydroleaf.store.model.OrderItem;
import se.hydroleaf.store.model.ShippingAddress;
import se.hydroleaf.store.model.StoreOrder;

public record OrderDetailsDTO(
        UUID orderId,
        String orderNumber,
        Instant createdAt,
        String status,
        String email,
        long subtotalCents,
        long shippingCents,
        long taxCents,
        long totalCents,
        String currency,
        ShippingAddressDTO shippingAddress,
        List<OrderItemDTO> items
) {

    public static OrderDetailsDTO from(StoreOrder order) {
        return new OrderDetailsDTO(
                order.getId(),
                order.getOrderNumber(),
                order.getCreatedAt(),
                order.getStatus().name(),
                order.getEmail(),
                order.getSubtotalCents(),
                order.getShippingCents(),
                order.getTaxCents(),
                order.getTotalCents(),
                order.getCurrency(),
                ShippingAddressDTO.from(order.getShippingAddress()),
                OrderItemDTO.from(order.getItems())
        );
    }

    public record ShippingAddressDTO(
            String name,
            String line1,
            String line2,
            String city,
            String state,
            String postalCode,
            String country,
            String phone
    ) {
        public static ShippingAddressDTO from(ShippingAddress address) {
            if (address == null) {
                return null;
            }
            return new ShippingAddressDTO(
                    address.getName(),
                    address.getLine1(),
                    address.getLine2(),
                    address.getCity(),
                    address.getState(),
                    address.getPostalCode(),
                    address.getCountry(),
                    address.getPhone()
            );
        }
    }

    public record OrderItemDTO(
            UUID id,
            UUID productId,
            UUID variantId,
            String name,
            long unitPriceCents,
            int quantity,
            long lineTotalCents
    ) {
        public static OrderItemDTO from(OrderItem item) {
            UUID productId = item.getProduct() != null ? item.getProduct().getId() : null;
            UUID variantId = item.getVariant() != null ? item.getVariant().getId() : null;
            return new OrderItemDTO(
                    item.getId(),
                    productId,
                    variantId,
                    item.getNameSnapshot(),
                    item.getUnitPriceCents(),
                    item.getQty(),
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
}
