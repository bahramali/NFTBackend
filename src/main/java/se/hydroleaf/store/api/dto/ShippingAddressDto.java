package se.hydroleaf.store.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShippingAddressDto {

    @NotBlank
    private String name;

    @NotBlank
    @JsonAlias("addressLine1")
    private String line1;

    @JsonAlias("addressLine2")
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
