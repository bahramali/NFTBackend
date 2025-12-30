package se.hydroleaf.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @Size(max = 128) String fullName,
        @NotBlank @Size(max = 128) String street1,
        @Size(max = 128) String street2,
        @NotBlank @Size(max = 16) String postalCode,
        @NotBlank @Size(max = 64) String city,
        @Size(max = 64) String region,
        @Size(max = 2) String countryCode,
        @Size(max = 32)
        @Pattern(
                regexp = "^[0-9+()\\-\\s]{0,32}$",
                message = "Phone may only include digits, spaces, +, -, and parentheses"
        )
        String phoneNumber,
        Boolean isDefault
) {
}
