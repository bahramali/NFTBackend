package se.hydroleaf.store.api.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CartItemResponse {
    UUID id;
    UUID productId;
    String sku;
    String name;
    int qty;
    long unitPriceCents;
    long lineTotalCents;
    String imageUrl;
    String currency;
}
