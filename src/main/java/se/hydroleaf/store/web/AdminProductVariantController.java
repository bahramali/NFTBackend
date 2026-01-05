package se.hydroleaf.store.web;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.model.Permission;
import se.hydroleaf.service.AuthenticatedUser;
import se.hydroleaf.service.AuthorizationService;
import se.hydroleaf.store.api.dto.ProductVariantAdminResponse;
import se.hydroleaf.store.api.dto.ProductVariantRequest;
import se.hydroleaf.store.service.ProductVariantService;

@RestController
@RequestMapping("/api/admin/store/products/{productId}/variants")
@RequiredArgsConstructor
public class AdminProductVariantController {

    private final AuthorizationService authorizationService;
    private final ProductVariantService productVariantService;

    private void requireStoreView(String token) {
        AuthenticatedUser user = authorizationService.requireAuthenticated(token);
        authorizationService.requirePermission(user, Permission.STORE_VIEW);
    }

    private void requireProductsManage(String token) {
        AuthenticatedUser user = authorizationService.requireAuthenticated(token);
        authorizationService.requirePermission(user, Permission.PRODUCTS_MANAGE);
    }

    @GetMapping
    public ResponseEntity<List<ProductVariantAdminResponse>> list(@RequestHeader(name = "Authorization", required = false) String token,
                                                                  @PathVariable UUID productId) {
        requireStoreView(token);
        return ResponseEntity.ok(productVariantService.listVariants(productId));
    }

    @PostMapping(consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductVariantAdminResponse> create(@RequestHeader(name = "Authorization", required = false) String token,
                                                              @PathVariable UUID productId,
                                                              @Valid @RequestBody ProductVariantRequest request) {
        requireProductsManage(token);
        ProductVariantAdminResponse response = productVariantService.createVariant(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/{variantId}", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductVariantAdminResponse> update(@RequestHeader(name = "Authorization", required = false) String token,
                                                              @PathVariable UUID productId,
                                                              @PathVariable UUID variantId,
                                                              @Valid @RequestBody ProductVariantRequest request) {
        requireProductsManage(token);
        return ResponseEntity.ok(productVariantService.updateVariant(productId, variantId, request));
    }

    @DeleteMapping("/{variantId}")
    public ResponseEntity<Void> delete(@RequestHeader(name = "Authorization", required = false) String token,
                                       @PathVariable UUID productId,
                                       @PathVariable UUID variantId) {
        requireProductsManage(token);
        productVariantService.deleteVariant(productId, variantId);
        return ResponseEntity.noContent().build();
    }
}
