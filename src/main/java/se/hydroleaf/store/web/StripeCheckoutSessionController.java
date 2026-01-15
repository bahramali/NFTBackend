package se.hydroleaf.store.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.service.AuthenticatedUser;
import se.hydroleaf.store.api.dto.StripeCheckoutSessionRequest;
import se.hydroleaf.store.api.dto.StripeCheckoutSessionResponse;
import se.hydroleaf.store.service.StripeCartCheckoutSessionService;

@RestController
@RequestMapping("/api/store/checkout/stripe")
@RequiredArgsConstructor
public class StripeCheckoutSessionController {

    private static final Logger log = LoggerFactory.getLogger(StripeCheckoutSessionController.class);
    private final StripeCartCheckoutSessionService stripeCartCheckoutSessionService;

    @PostMapping("/session")
    public ResponseEntity<StripeCheckoutSessionResponse> createSession(
            Authentication authentication,
            @Valid @RequestBody StripeCheckoutSessionRequest request
    ) {
        AuthenticatedUser user = requireAuthenticated(authentication);
        StripeCartCheckoutSessionService.StripeCheckoutSessionResult result =
                stripeCartCheckoutSessionService.createCheckoutSession(user, request);
        return ResponseEntity.ok(new StripeCheckoutSessionResponse(result.checkoutUrl()));
    }

    private AuthenticatedUser requireAuthenticated(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Stripe checkout session auth failed: missing authentication");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AuthenticatedUser user)) {
            String principalType = principal == null ? "null" : principal.getClass().getSimpleName();
            log.warn("Stripe checkout session auth failed: unexpected principal type={}", principalType);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return user;
    }
}
