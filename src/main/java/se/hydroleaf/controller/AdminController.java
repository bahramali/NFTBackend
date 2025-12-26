package se.hydroleaf.controller;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.controller.dto.PermissionCatalogItem;
import se.hydroleaf.model.Permission;
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
        authorizationService.requirePermission(user, Permission.ADMIN_OVERVIEW_VIEW);
        return Map.of("orders", 42, "revenue", 12345.67, "alerts", 1);
    }

    @GetMapping("/orders")
    public Map<String, Object> manageOrders(@RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser user = authorizationService.requireAuthenticated(token);
        authorizationService.requirePermission(user, Permission.ORDERS_MANAGE);
        return Map.of("openOrders", 5, "recentAction", "Orders accessible");
    }

    @GetMapping("/permissions")
    public Map<String, Object> permissionsCatalog(@RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser user = authorizationService.requireAuthenticated(token);
        authorizationService.requirePermission(user, Permission.ADMIN_INVITE);
        Map<String, List<PermissionCatalogItem>> permissions = Arrays.stream(Permission.values())
                .map(permission -> new PermissionCatalogItem(
                        permission.name(),
                        permission.label(),
                        permission.group()))
                .collect(Collectors.groupingBy(
                        PermissionCatalogItem::group,
                        LinkedHashMap::new,
                        Collectors.toList()));
        Map<String, List<String>> presets = Arrays.stream(se.hydroleaf.model.AdminPreset.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        preset -> preset.permissions().stream().map(Enum::name).toList(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));
        return Map.of("permissions", permissions, "presets", presets);
    }
}
