package se.hydroleaf.store.api.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProductResponse {
    UUID id;
    String sku;
    String name;
    String description;
    long priceCents;
    String currency;
    boolean active;
    int inventoryQty;
    String imageUrl;
    String category;
    Instant createdAt;
    Instant updatedAt;
}
