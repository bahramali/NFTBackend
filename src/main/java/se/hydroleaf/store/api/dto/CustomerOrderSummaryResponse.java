package se.hydroleaf.store.api.dto;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CustomerOrderSummaryResponse {
    UUID id;
    LocalDate date;
    long total;
    String status;
}
