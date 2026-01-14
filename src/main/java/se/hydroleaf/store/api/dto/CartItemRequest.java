package se.hydroleaf.store.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartItemRequest {

    @NotNull(message = "variantId is required")
    @JsonAlias({ "variant_id", "itemId" })
    private UUID variantId;

    @Min(1)
    @JsonAlias("quantity")
    private int qty;
}
