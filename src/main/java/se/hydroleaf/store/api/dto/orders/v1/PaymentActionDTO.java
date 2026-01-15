package se.hydroleaf.store.api.dto.orders.v1;

public record PaymentActionDTO(
        String type,
        String label,
        String url
) {
}
