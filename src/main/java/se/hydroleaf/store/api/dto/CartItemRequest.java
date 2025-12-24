package se.hydroleaf.store.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartItemRequest {

    @NotNull
    private UUID productId;

    @Min(1)
    private Integer qty;
}
