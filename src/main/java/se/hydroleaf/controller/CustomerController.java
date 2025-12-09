package se.hydroleaf.controller;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.service.AuthService;
import se.hydroleaf.service.AuthenticatedUser;
import se.hydroleaf.service.AuthorizationService;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final AuthService authService;
    private final AuthorizationService authorizationService;

    @GetMapping("/me")
    public Map<String, Object> me(@RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser user = authenticate(token);
        authorizationService.requireRole(user, UserRole.CUSTOMER);
        authorizationService.requireSelfAccess(user, user.userId());
        return Map.of("userId", user.userId(), "profile", "Customer profile data");
    }

    @GetMapping("/orders")
    public List<Map<String, Object>> myOrders(@RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser user = authenticate(token);
        authorizationService.requireRole(user, UserRole.CUSTOMER);
        authorizationService.requireSelfAccess(user, user.userId());
        return List.of(
                Map.of("orderId", 101, "customerId", user.userId(), "status", "DELIVERED"),
                Map.of("orderId", 102, "customerId", user.userId(), "status", "PROCESSING")
        );
    }

    private AuthenticatedUser authenticate(String token) {
        try {
            return authService.authenticate(token);
        } catch (SecurityException se) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, se.getMessage(), se);
        }
    }
}
