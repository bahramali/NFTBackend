package se.hydroleaf.store.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequest {

    @NotNull
    private UUID cartId;

    @Email
    @NotBlank
    private String email;

    private UUID userId;

    @Valid
    @NotNull
    private ShippingAddressDto shippingAddress;
}
