package se.hydroleaf.store.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShippingAddressDto {

    @NotBlank
    private String name;

    @NotBlank
    private String line1;

    private String line2;

    @NotBlank
    private String city;

    private String state;

    @NotBlank
    private String postalCode;

    @NotBlank
    private String country;

    private String phone;
}
