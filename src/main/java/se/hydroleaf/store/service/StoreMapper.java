package se.hydroleaf.store.service;

import java.util.List;
import org.springframework.stereotype.Component;
import se.hydroleaf.store.api.dto.CartItemResponse;
import se.hydroleaf.store.api.dto.CartResponse;
import se.hydroleaf.store.api.dto.MoneySummary;
import se.hydroleaf.store.api.dto.ProductResponse;
import se.hydroleaf.store.model.Cart;
import se.hydroleaf.store.model.CartItem;
import se.hydroleaf.store.model.Product;

@Component
public class StoreMapper {

    public ProductResponse toProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .priceCents(product.getPriceCents())
                .currency(product.getCurrency())
                .active(product.isActive())
                .inventoryQty(product.getInventoryQty())
                .imageUrl(product.getImageUrl())
                .category(product.getCategory())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public CartResponse toCartResponse(Cart cart, MoneySummary totals) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::toCartItemResponse)
                .toList();

        return CartResponse.builder()
                .id(cart.getId())
                .sessionId(cart.getSessionId())
                .userId(cart.getUserId())
                .status(cart.getStatus())
                .items(items)
                .totals(totals)
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    private CartItemResponse toCartItemResponse(CartItem item) {
        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .sku(item.getProduct().getSku())
                .name(item.getProduct().getName())
                .qty(item.getQty())
                .unitPriceCents(item.getUnitPriceCents())
                .lineTotalCents(item.getLineTotalCents())
                .imageUrl(item.getProduct().getImageUrl())
                .currency(item.getProduct().getCurrency())
                .build();
    }
}
