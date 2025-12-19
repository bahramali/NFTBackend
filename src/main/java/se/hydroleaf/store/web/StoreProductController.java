package se.hydroleaf.store.web;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.store.api.dto.ProductResponse;
import se.hydroleaf.store.service.ProductService;

@RestController
@RequestMapping("/api/store/products")
@RequiredArgsConstructor
public class StoreProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> list(@RequestParam(value = "active", required = false) Boolean active) {
        return ResponseEntity.ok(productService.listProducts(active));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }
}
