package se.hydroleaf.controller;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.controller.dto.MyDeviceResponse;
import se.hydroleaf.controller.dto.MyOrderResponse;
import se.hydroleaf.controller.dto.MyProfileResponse;
import se.hydroleaf.service.MyAccountService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MyAccountController {

    private final MyAccountService myAccountService;

    @GetMapping("/me")
    public MyProfileResponse me(@RequestHeader(name = "Authorization", required = false) String token) {
        return myAccountService.getCurrentProfile(token);
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
    public List<MyOrderResponse> myOrders(@RequestHeader(name = "Authorization", required = false) String token) {
        return myAccountService.listMyOrders(token);
    }

    @GetMapping("/store/orders/{orderId}")
    public MyOrderResponse myOrder(
            @RequestHeader(name = "Authorization", required = false) String token,
            @PathVariable UUID orderId) {
        return myAccountService.getMyOrder(token, orderId);
    }
}
