package se.hydroleaf.store.web;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.model.Permission;
import se.hydroleaf.service.AuthenticatedUser;
import se.hydroleaf.service.AuthorizationService;
import se.hydroleaf.store.api.dto.ProductRequest;
import se.hydroleaf.store.api.dto.ProductResponse;
import se.hydroleaf.store.service.ProductService;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private static final Logger log = LoggerFactory.getLogger(AdminProductController.class);

    private final AuthorizationService authorizationService;
    private final ProductService productService;

    private void requireStoreView(String token) {
        AuthenticatedUser user = authorizationService.requireAuthenticated(token);
        authorizationService.requirePermission(user, Permission.STORE_VIEW);
    }

    private void requireProductsManage(String token) {
        AuthenticatedUser user = authorizationService.requireAuthenticated(token);
        authorizationService.requirePermission(user, Permission.PRODUCTS_MANAGE);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> list(@RequestHeader(name = "Authorization", required = false) String token,
                                                      @RequestParam(value = "active", required = false) Boolean active) {
        requireStoreView(token);
        return ResponseEntity.ok(productService.listProducts(active));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> get(@RequestHeader(name = "Authorization", required = false) String token,
                                               @PathVariable UUID id) {
        requireStoreView(token);
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @PostMapping(consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductResponse> create(@RequestHeader(name = "Authorization", required = false) String token,
                                                  @Valid @RequestBody ProductRequest request) {
        requireProductsManage(token);
        if (log.isDebugEnabled()) {
            log.debug("Create product request: {}", request);
        }
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/{id}", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductResponse> update(@RequestHeader(name = "Authorization", required = false) String token,
                                                  @PathVariable UUID id,
                                                  @Valid @RequestBody ProductRequest request) {
        requireProductsManage(token);
        if (log.isDebugEnabled()) {
            log.debug("Update product {} request: {}", id, request);
        }
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader(name = "Authorization", required = false) String token,
                                       @PathVariable UUID id) {
        requireProductsManage(token);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
