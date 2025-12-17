package se.hydroleaf.store.api.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MoneySummary {
    long subtotalCents;
    long shippingCents;
    long taxCents;
    long totalCents;
    String currency;
}
