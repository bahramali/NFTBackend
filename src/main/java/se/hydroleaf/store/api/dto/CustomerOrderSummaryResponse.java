package se.hydroleaf.store.api.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CustomerOrderSummaryResponse {
    UUID orderId;
    Instant createdAt;
    long total;
    String currency;
    String status;
    int itemsCount;
    int itemsQuantity;
}
