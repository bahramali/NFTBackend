package se.hydroleaf.store.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.model.Permission;
import se.hydroleaf.service.AuthenticatedUser;
import se.hydroleaf.service.AuthorizationService;
import se.hydroleaf.store.api.dto.CustomerDetailsResponse;
import se.hydroleaf.store.api.dto.CustomerListResponse;
import se.hydroleaf.store.service.CustomerService;

@RestController
@RequestMapping("/api/admin/customers")
@RequiredArgsConstructor
public class AdminCustomerController {

    private final AuthorizationService authorizationService;
    private final CustomerService customerService;

    private void requireCustomersView(String token) {
        AuthenticatedUser user = authorizationService.requireAuthenticated(token);
        authorizationService.requirePermission(user, Permission.CUSTOMERS_VIEW);
    }

    @GetMapping
    public ResponseEntity<CustomerListResponse> list(
            @RequestHeader(name = "Authorization", required = false) String token,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size) {
        requireCustomersView(token);
        return ResponseEntity.ok(customerService.listCustomers(query, status, type, sort, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDetailsResponse> get(@RequestHeader(name = "Authorization", required = false) String token,
                                                       @PathVariable String id) {
        requireCustomersView(token);
        return ResponseEntity.ok(customerService.getCustomerDetails(id));
    }
}
