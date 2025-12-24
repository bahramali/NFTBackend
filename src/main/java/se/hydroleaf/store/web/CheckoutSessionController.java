package se.hydroleaf.store.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.store.api.dto.CheckoutSessionRequest;
import se.hydroleaf.store.api.dto.CheckoutSessionResponse;
import se.hydroleaf.store.service.CheckoutSessionService;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutSessionController {

    private final CheckoutSessionService checkoutSessionService;

    @PostMapping("/sessions")
    public ResponseEntity<CheckoutSessionResponse> createSession(@Valid @RequestBody CheckoutSessionRequest request) {
        CheckoutSessionService.CheckoutSessionResult result = checkoutSessionService.createSession(request.getOrderId());
        return ResponseEntity.ok(CheckoutSessionResponse.builder()
                .redirectUrl(result.redirectUrl())
                .paymentId(result.paymentId())
                .build());
    }
}
