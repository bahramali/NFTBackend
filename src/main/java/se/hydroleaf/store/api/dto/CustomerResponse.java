package se.hydroleaf.store.api.dto;

import lombok.Builder;
import lombok.Value;
import java.time.Instant;

@Value
@Builder
public class CustomerResponse {
    String id;
    String name;
    String email;
    String customerType;
    int ordersCount;
    long totalSpent;
    String currency;
    Instant lastOrderAt;
    String status;
}
