package se.hydroleaf.store.api.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CheckoutSessionResponse {

    private final String redirectUrl;
    private final UUID paymentId;
}
