package se.hydroleaf.store.service;

import java.util.List;
import org.springframework.stereotype.Component;
import se.hydroleaf.store.api.dto.CartItemResponse;
import se.hydroleaf.store.api.dto.CartResponse;
import se.hydroleaf.store.api.dto.MoneySummary;
import se.hydroleaf.store.api.dto.ProductResponse;
import se.hydroleaf.store.api.dto.ProductVariantResponse;
import se.hydroleaf.store.model.Cart;
import se.hydroleaf.store.model.CartItem;
import se.hydroleaf.store.model.Product;
import se.hydroleaf.store.model.ProductVariant;

@Component
public class StoreMapper {

    public ProductResponse toProductResponse(Product product) {
        List<ProductVariantResponse> variants = product.getVariants().stream()
                .map(this::toProductVariantResponse)
                .toList();
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
                .variants(variants)
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
                .variantId(item.getVariant() == null ? null : item.getVariant().getId())
                .sku(item.getProduct().getSku())
                .name(item.getProduct().getName())
                .variantLabel(item.getVariant() == null ? null : item.getVariant().getLabel())
                .weightGrams(item.getVariant() == null ? null : item.getVariant().getWeightGrams())
                .qty(item.getQty())
                .unitPriceCents(item.getUnitPriceCents())
                .lineTotalCents(item.getLineTotalCents())
                .imageUrl(item.getProduct().getImageUrl())
                .currency(item.getProduct().getCurrency())
                .build();
    }

    private ProductVariantResponse toProductVariantResponse(ProductVariant variant) {
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .label(variant.getLabel())
                .weightGrams(variant.getWeightGrams())
                .priceCents(variant.getPriceCents())
                .stockQuantity(variant.getStockQuantity())
                .active(variant.isActive())
                .build();
    }
}
