package se.hydroleaf.store.api.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CustomerResponse {
    String id;
    String name;
    String email;
    String customerType;
    int ordersCount;
    long totalSpent;
    LocalDate lastOrderAt;
    String status;
}
