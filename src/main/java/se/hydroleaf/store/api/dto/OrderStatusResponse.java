package se.hydroleaf.store.api.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderStatusResponse {

    private final UUID orderId;
    private final String orderStatus;
    private final String paymentStatus;
    private final String displayStatus;
    private final long totalAmountCents;
    private final String currency;
}
