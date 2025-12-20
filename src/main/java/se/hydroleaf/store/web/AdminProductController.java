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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.service.AuthorizationService;
import se.hydroleaf.store.api.dto.ProductRequest;
import se.hydroleaf.store.api.dto.ProductResponse;
import se.hydroleaf.store.service.ProductService;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AuthorizationService authorizationService;
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> list(@RequestHeader(name = "Authorization", required = false) String token,
                                                      @RequestParam(value = "active", required = false) Boolean active) {
        authorizationService.requireAdminOrOperator(token);
        return ResponseEntity.ok(productService.listProducts(active));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> get(@RequestHeader(name = "Authorization", required = false) String token,
                                               @PathVariable UUID id) {
        authorizationService.requireAdminOrOperator(token);
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@RequestHeader(name = "Authorization", required = false) String token,
                                                  @Valid @RequestBody ProductRequest request) {
        authorizationService.requireAdminOrOperator(token);
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@RequestHeader(name = "Authorization", required = false) String token,
                                                  @PathVariable UUID id,
                                                  @Valid @RequestBody ProductRequest request) {
        authorizationService.requireAdminOrOperator(token);
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader(name = "Authorization", required = false) String token,
                                       @PathVariable UUID id) {
        authorizationService.requireAdminOrOperator(token);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
