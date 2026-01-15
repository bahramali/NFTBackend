package se.hydroleaf.store.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class StripeCheckoutSessionRequest {

    @NotNull
    private UUID cartId;

    @Email
    @NotBlank
    private String email;

    @Valid
    @NotNull
    private ShippingAddressDto shippingAddress;
}
