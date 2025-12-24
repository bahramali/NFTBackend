package se.hydroleaf.store.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutSessionRequest {

    @NotNull
    private UUID orderId;
}
