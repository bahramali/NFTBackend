package se.hydroleaf.store.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequest {

    @NotBlank
    private String sku;

    @NotBlank
    private String name;

    private String description;

    @Min(0)
    private long priceCents;

    @Size(min = 3, max = 3)
    private String currency;

    private boolean active = true;

    @Min(0)
    private int inventoryQty;

    private String imageUrl;

    private String category;
}
