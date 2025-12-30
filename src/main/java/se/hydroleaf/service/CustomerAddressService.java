package se.hydroleaf.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.controller.dto.AddressRequest;
import se.hydroleaf.controller.dto.AddressResponse;
import se.hydroleaf.model.CustomerAddress;
import se.hydroleaf.model.User;
import se.hydroleaf.repository.CustomerAddressRepository;
import se.hydroleaf.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerAddressService {

    private static final String DEFAULT_COUNTRY_CODE = "SE";

    private final CustomerAddressRepository customerAddressRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(AuthenticatedUser user) {
        return customerAddressRepository.findByUserIdOrderByCreatedAtDesc(user.userId()).stream()
                .map(AddressResponse::from)
                .toList();
    }

    @Transactional
    public AddressResponse createAddress(AuthenticatedUser user, AddressRequest request) {
        User owner = userRepository.findById(user.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        boolean shouldDefault = Boolean.TRUE.equals(request.isDefault())
                || customerAddressRepository.countByUserId(user.userId()) == 0;
        CustomerAddress address = CustomerAddress.builder()
                .user(owner)
                .fullName(trimOrNull(request.fullName()))
                .street1(trimRequired(request.street1()))
                .street2(trimOrNull(request.street2()))
                .postalCode(trimRequired(request.postalCode()))
                .city(trimRequired(request.city()))
                .region(trimOrNull(request.region()))
                .countryCode(normalizeCountryCode(request.countryCode()))
                .phoneNumber(trimOrNull(request.phoneNumber()))
                .isDefault(shouldDefault)
                .build();
        CustomerAddress saved = customerAddressRepository.save(address);
        if (shouldDefault) {
            customerAddressRepository.clearDefaultForUser(user.userId(), saved.getId());
        }
        log.info("CustomerAddressService.createAddress userId={} addressId={}", user.userId(), saved.getId());
        return AddressResponse.from(saved);
    }

    @Transactional
    public AddressResponse updateAddress(AuthenticatedUser user, Long addressId, AddressRequest request) {
        CustomerAddress address = customerAddressRepository.findByIdAndUserId(addressId, user.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
        address.setFullName(trimOrNull(request.fullName()));
        address.setStreet1(trimRequired(request.street1()));
        address.setStreet2(trimOrNull(request.street2()));
        address.setPostalCode(trimRequired(request.postalCode()));
        address.setCity(trimRequired(request.city()));
        address.setRegion(trimOrNull(request.region()));
        address.setCountryCode(normalizeCountryCode(request.countryCode()));
        address.setPhoneNumber(trimOrNull(request.phoneNumber()));
        if (Boolean.TRUE.equals(request.isDefault())) {
            customerAddressRepository.clearDefaultForUser(user.userId(), address.getId());
            address.setDefault(true);
        }
        CustomerAddress saved = customerAddressRepository.save(address);
        log.info("CustomerAddressService.updateAddress userId={} addressId={}", user.userId(), saved.getId());
        return AddressResponse.from(saved);
    }

    @Transactional
    public void deleteAddress(AuthenticatedUser user, Long addressId) {
        CustomerAddress address = customerAddressRepository.findByIdAndUserId(addressId, user.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
        boolean wasDefault = address.isDefault();
        customerAddressRepository.delete(address);
        if (wasDefault) {
            customerAddressRepository.findFirstByUserIdOrderByCreatedAtDesc(user.userId())
                    .ifPresent(replacement -> {
                        customerAddressRepository.clearDefaultForUser(user.userId(), replacement.getId());
                        replacement.setDefault(true);
                        customerAddressRepository.save(replacement);
                    });
        }
        log.info("CustomerAddressService.deleteAddress userId={} addressId={}", user.userId(), addressId);
    }

    @Transactional
    public AddressResponse setDefaultAddress(AuthenticatedUser user, Long addressId) {
        CustomerAddress address = customerAddressRepository.findByIdAndUserId(addressId, user.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
        customerAddressRepository.clearDefaultForUser(user.userId(), address.getId());
        address.setDefault(true);
        CustomerAddress saved = customerAddressRepository.save(address);
        log.info("CustomerAddressService.setDefaultAddress userId={} addressId={}", user.userId(), saved.getId());
        return AddressResponse.from(saved);
    }

    private String normalizeCountryCode(String countryCode) {
        String trimmed = trimOrNull(countryCode);
        if (trimmed == null || trimmed.isBlank()) {
            return DEFAULT_COUNTRY_CODE;
        }
        return trimmed.toUpperCase();
    }

    private String trimRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
