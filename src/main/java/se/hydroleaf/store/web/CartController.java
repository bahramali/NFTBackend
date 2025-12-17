package se.hydroleaf.store.web;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.store.api.dto.CartCreateRequest;
import se.hydroleaf.store.api.dto.CartItemRequest;
import se.hydroleaf.store.api.dto.CartResponse;
import se.hydroleaf.store.api.dto.UpdateCartItemRequest;
import se.hydroleaf.store.service.CartService;

@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/cart")
    public ResponseEntity<CartResponse> createCart(@RequestBody(required = false) CartCreateRequest request) {
        if (request == null) {
            request = new CartCreateRequest();
        }
        return ResponseEntity.ok(cartService.createCart(request));
    }

    @GetMapping("/cart/{cartId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable UUID cartId) {
        return ResponseEntity.ok(cartService.getCart(cartId));
    }

    @PostMapping("/cart/{cartId}/items")
    public ResponseEntity<CartResponse> addItem(@PathVariable UUID cartId, @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(cartId, request));
    }

    @PatchMapping("/cart/{cartId}/items/{itemId}")
    public ResponseEntity<CartResponse> updateItem(@PathVariable UUID cartId, @PathVariable UUID itemId, @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(cartService.updateItem(cartId, itemId, request));
    }

    @DeleteMapping("/cart/{cartId}/items/{itemId}")
    public ResponseEntity<CartResponse> deleteItem(@PathVariable UUID cartId, @PathVariable UUID itemId) {
        return ResponseEntity.ok(cartService.removeItem(cartId, itemId));
    }
}
