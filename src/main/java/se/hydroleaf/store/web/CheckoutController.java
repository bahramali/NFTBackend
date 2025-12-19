package se.hydroleaf.store.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.store.api.dto.CheckoutRequest;
import se.hydroleaf.store.api.dto.CheckoutResponse;
import se.hydroleaf.store.service.CheckoutService;

@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(checkoutService.checkout(request));
    }
}
