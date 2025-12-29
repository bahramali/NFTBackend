package se.hydroleaf.store.api.dto;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CustomerDetailsResponse {
    String id;
    String name;
    String email;
    String customerType;
    String status;
    Instant createdAt;
    Instant lastLoginAt;
    Instant lastSeenAt;
    long totalSpent;
    String currency;
    Instant lastOrderAt;
    Profile profile;
    List<CustomerOrderSummaryResponse> orders;

    @Value
    @Builder
    public static class Profile {
        String name;
        String email;
        String phone;
    }
}
