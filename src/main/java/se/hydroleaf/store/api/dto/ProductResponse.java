package se.hydroleaf.store.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
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

    @JsonProperty("price")
    public BigDecimal price() {
        return BigDecimal.valueOf(priceCents, 2);
    }

    @JsonProperty("stock")
    public int stock() {
        return inventoryQty;
    }
}
