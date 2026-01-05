package se.hydroleaf.store.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductVariantRequest {

    @Min(value = 1, message = "Weight grams must be greater than zero")
    private int weightGrams;

    @Size(max = 64, message = "Label must be at most 64 characters")
    private String label;

    @Min(value = 0, message = "Price must be zero or positive")
    private long priceCents;

    @Min(value = 0, message = "Stock quantity must be zero or positive")
    private int stockQuantity;

    @Size(max = 64, message = "SKU must be at most 64 characters")
    private String sku;

    private String ean;

    private boolean active = true;

    @JsonSetter("priceSek")
    void setDecimalPriceSek(BigDecimal priceSek) {
        if (priceSek != null) {
            this.priceCents = priceSek.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        }
    }

    @JsonSetter("price")
    void setDecimalPrice(BigDecimal price) {
        if (price != null) {
            this.priceCents = price.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        }
    }

    @JsonSetter("weight")
    void setWeightAlias(Integer weight) {
        if (weight != null) {
            this.weightGrams = weight;
        }
    }

    @JsonSetter("stock")
    void setStockAlias(Integer stock) {
        if (stock != null) {
            this.stockQuantity = stock;
        }
    }
}
