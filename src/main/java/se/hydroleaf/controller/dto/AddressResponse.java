package se.hydroleaf.controller.dto;

import java.time.LocalDateTime;
import se.hydroleaf.model.CustomerAddress;

public record AddressResponse(
        Long id,
        String label,
        String fullName,
        String street1,
        String street2,
        String postalCode,
        String city,
        String region,
        String countryCode,
        String phoneNumber,
        boolean isDefault,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AddressResponse from(CustomerAddress address) {
        return new AddressResponse(
                address.getId(),
                address.getLabel(),
                address.getFullName(),
                address.getStreet1(),
                address.getStreet2(),
                address.getPostalCode(),
                address.getCity(),
                address.getRegion(),
                address.getCountryCode(),
                address.getPhoneNumber(),
                address.isDefault(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }
}
