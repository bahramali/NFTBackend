package se.hydroleaf.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.service.AuthenticatedUser;
import se.hydroleaf.service.AuthorizationService;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthorizationService authorizationService;

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(@RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser user = authorizationService.requireAuthenticated(token);
        authorizationService.requirePermission(user, Permission.ADMIN_DASHBOARD);
        return Map.of("orders", 42, "revenue", 12345.67, "alerts", 1);
    }

    @GetMapping("/orders")
    public Map<String, Object> manageOrders(@RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser user = authorizationService.requireAuthenticated(token);
        authorizationService.requirePermission(user, Permission.REPORTS);
        return Map.of("openOrders", 5, "recentAction", "Orders accessible");
    }

    @GetMapping("/permissions")
    public Map<String, Object> permissions(@RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser user = authorizationService.requireAuthenticated(token);
        authorizationService.requireRole(user, UserRole.ADMIN);
        List<String> available = Arrays.stream(Permission.values())
                .map(Enum::name)
                .toList();
        List<String> granted = user.permissions().stream()
                .map(Enum::name)
                .toList();
        return Map.of("available", available, "granted", granted);
    }
}
