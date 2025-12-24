package se.hydroleaf.store.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
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

    @JsonProperty("subtotal")
    public BigDecimal subtotal() {
        return BigDecimal.valueOf(subtotalCents, 2);
    }

    @JsonProperty("shipping")
    public BigDecimal shipping() {
        return BigDecimal.valueOf(shippingCents, 2);
    }

    @JsonProperty("tax")
    public BigDecimal tax() {
        return BigDecimal.valueOf(taxCents, 2);
    }

    @JsonProperty("total")
    public BigDecimal total() {
        return BigDecimal.valueOf(totalCents, 2);
    }
}
