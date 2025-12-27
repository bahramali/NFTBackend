package se.hydroleaf.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.service.AuthenticatedUser;
import se.hydroleaf.service.AuthorizationService;
import se.hydroleaf.store.api.dto.CustomersPageResponse;
import se.hydroleaf.store.service.AdminCustomerService;

@RestController
@RequestMapping("/api/admin/customers")
@RequiredArgsConstructor
public class AdminCustomerController {

    private final AuthorizationService authorizationService;
    private final AdminCustomerService adminCustomerService;

    @GetMapping
    public CustomersPageResponse list(
            @RequestHeader(name = "Authorization", required = false) String token,
            @RequestParam(defaultValue = "last_order_desc") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AuthenticatedUser user = authorizationService.requireAuthenticated(token);
        authorizationService.requireRoleOrPermission(
                user,
                Permission.CUSTOMERS_VIEW,
                UserRole.ADMIN
        );

        return adminCustomerService.list(sort, page, size);
    }
}