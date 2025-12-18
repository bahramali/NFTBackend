package se.hydroleaf.store.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import se.hydroleaf.store.model.CartStatus;

@Value
@Builder
public class CartResponse {
    UUID id;
    String sessionId;
    UUID userId;
    CartStatus status;
    List<CartItemResponse> items;
    MoneySummary totals;
    Instant updatedAt;
}
