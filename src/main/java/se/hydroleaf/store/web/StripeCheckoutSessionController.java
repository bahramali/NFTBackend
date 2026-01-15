package se.hydroleaf.store.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.service.AuthenticatedUser;
import se.hydroleaf.service.AuthorizationService;
import se.hydroleaf.store.api.dto.StripeCheckoutSessionRequest;
import se.hydroleaf.store.api.dto.StripeCheckoutSessionResponse;
import se.hydroleaf.store.service.StripeCartCheckoutSessionService;

@RestController
@RequestMapping("/api/store/checkout/stripe")
@RequiredArgsConstructor
public class StripeCheckoutSessionController {

    private final AuthorizationService authorizationService;
    private final StripeCartCheckoutSessionService stripeCartCheckoutSessionService;

    @PostMapping("/session")
    public ResponseEntity<StripeCheckoutSessionResponse> createSession(
            @RequestHeader(name = "Authorization", required = false) String token,
            @Valid @RequestBody StripeCheckoutSessionRequest request
    ) {
        AuthenticatedUser user = authorizationService.requireAuthenticated(token);
        StripeCartCheckoutSessionService.StripeCheckoutSessionResult result =
                stripeCartCheckoutSessionService.createCheckoutSession(user, request);
        return ResponseEntity.ok(new StripeCheckoutSessionResponse(result.checkoutUrl()));
    }
}
