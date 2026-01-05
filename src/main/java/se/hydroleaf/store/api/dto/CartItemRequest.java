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

    @NotNull
    @JsonAlias("variant_id")
    private UUID variantId;

    @Min(1)
    @JsonAlias("quantity")
    private Integer qty;
}
