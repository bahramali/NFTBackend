package se.hydroleaf.store.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProductVariantAdminResponse {
    UUID id;
    String label;
    int weightGrams;
    long priceCents;
    int stockQuantity;
    String sku;
    String ean;
    boolean active;

    @JsonProperty("priceSek")
    public BigDecimal priceSek() {
        return BigDecimal.valueOf(priceCents, 2);
    }
}
