package se.hydroleaf.controller;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.controller.dto.AddressRequest;
import se.hydroleaf.controller.dto.AddressResponse;
import se.hydroleaf.controller.dto.MyDeviceResponse;
import se.hydroleaf.store.api.dto.orders.v1.OrderDetailsDTO;
import se.hydroleaf.store.api.dto.orders.v1.OrderSummaryDTO;
import se.hydroleaf.controller.dto.MyProfileResponse;
import se.hydroleaf.controller.dto.UpdateMyProfileRequest;
import se.hydroleaf.service.AuthenticatedUser;
import se.hydroleaf.service.AuthorizationService;
import se.hydroleaf.service.CustomerAddressService;
import se.hydroleaf.service.MyAccountService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class MyAccountController {

    private final MyAccountService myAccountService;
    private final AuthorizationService authorizationService;
    private final CustomerAddressService customerAddressService;

    @GetMapping("/me")
    public MyProfileResponse me(@RequestHeader(name = "Authorization", required = false) String token) {
        return myAccountService.getCurrentProfile(token);
    }

    @PutMapping({"/me", "/me/", "/me/profile"})
    public MyProfileResponse updateMe(
            @RequestHeader(name = "Authorization", required = false) String token,
            @Valid @RequestBody UpdateMyProfileRequest request,
            HttpServletRequest httpRequest) {
        AuthenticatedUser user = authorizationService.requireAuthenticated(token);
        long bodySize = httpRequest.getContentLengthLong();
        log.info(
                "MyAccountController.updateMe received method={} uri={} principal={} bodySize={}",
                httpRequest.getMethod(),
                httpRequest.getRequestURI(),
                user.userId(),
                bodySize);
        return myAccountService.updateCurrentProfile(user, request);
    }

    @GetMapping("/my/devices")
    public List<MyDeviceResponse> myDevices(@RequestHeader(name = "Authorization", required = false) String token) {
        return myAccountService.listMyDevices(token);
    }

    @GetMapping("/my/devices/{deviceId}")
    public MyDeviceResponse myDevice(
            @RequestHeader(name = "Authorization", required = false) String token,
            @PathVariable String deviceId) {
        return myAccountService.getMyDevice(token, deviceId);
    }

    @GetMapping("/store/orders/my")
    public List<OrderSummaryDTO> myOrders(@RequestHeader(name = "Authorization", required = false) String token) {
        return myAccountService.listMyOrders(token);
    }

    @GetMapping("/store/orders/{orderId}")
    public OrderDetailsDTO myOrder(
            @RequestHeader(name = "Authorization", required = false) String token,
            @PathVariable UUID orderId) {
        return myAccountService.getMyOrder(token, orderId);
    }

    @GetMapping("/me/addresses")
    public List<AddressResponse> listAddresses(@RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser user = authorizationService.requireAuthenticated(token);
        return customerAddressService.listAddresses(user);
    }

    @PostMapping("/me/addresses")
    public AddressResponse createAddress(
            @RequestHeader(name = "Authorization", required = false) String token,
            @Valid @RequestBody AddressRequest request) {
        AuthenticatedUser user = authorizationService.requireAuthenticated(token);
        return customerAddressService.createAddress(user, request);
    }

    @PutMapping("/me/addresses/{id}")
    public AddressResponse updateAddress(
            @RequestHeader(name = "Authorization", required = false) String token,
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request) {
        AuthenticatedUser user = authorizationService.requireAuthenticated(token);
        return customerAddressService.updateAddress(user, id, request);
    }

    @DeleteMapping("/me/addresses/{id}")
    public void deleteAddress(
            @RequestHeader(name = "Authorization", required = false) String token,
            @PathVariable Long id) {
        AuthenticatedUser user = authorizationService.requireAuthenticated(token);
        customerAddressService.deleteAddress(user, id);
    }

    @PutMapping("/me/addresses/{id}/default")
    public AddressResponse setDefaultAddress(
            @RequestHeader(name = "Authorization", required = false) String token,
            @PathVariable Long id) {
        AuthenticatedUser user = authorizationService.requireAuthenticated(token);
        return customerAddressService.setDefaultAddress(user, id);
    }
}
