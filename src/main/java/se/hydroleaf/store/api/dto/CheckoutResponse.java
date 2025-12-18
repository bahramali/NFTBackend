package se.hydroleaf.store.api.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CheckoutResponse {
    UUID orderId;
    String paymentUrl;
}
