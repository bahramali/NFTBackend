package se.hydroleaf.store.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class ProductRequest {

    @NotBlank(message = "SKU is required")
    private String sku;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @Min(value = 0, message = "Price must be zero or positive")
    private long priceCents;

    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    private String currency;

    private boolean active = true;

    @Min(value = 0, message = "Inventory quantity must be zero or positive")
    private int inventoryQty;

    private String imageUrl;

    private String category;

    @JsonSetter("price")
    void setDecimalPrice(BigDecimal price) {
        if (price != null) {
            this.priceCents = price.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        }
    }

    @JsonSetter("stock")
    void setStock(Integer stock) {
        if (stock != null) {
            this.inventoryQty = stock;
        }
    }
}
