package se.hydroleaf.store.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CartItemResponse {
    UUID id;
    UUID productId;
    UUID variantId;
    String sku;
    String name;
    String variantLabel;
    Integer weightGrams;
    int qty;
    long unitPriceCents;
    long lineTotalCents;
    String imageUrl;
    String currency;

    @JsonProperty("price")
    public BigDecimal price() {
        return BigDecimal.valueOf(unitPriceCents, 2);
    }

    @JsonProperty("unitPriceSek")
    public BigDecimal unitPriceSek() {
        return BigDecimal.valueOf(unitPriceCents, 2);
    }

    @JsonProperty("lineTotal")
    public BigDecimal lineTotal() {
        return BigDecimal.valueOf(lineTotalCents, 2);
    }

    @JsonProperty("lineTotalSek")
    public BigDecimal lineTotalSek() {
        return BigDecimal.valueOf(lineTotalCents, 2);
    }
}
