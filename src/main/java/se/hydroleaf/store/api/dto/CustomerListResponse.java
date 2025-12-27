package se.hydroleaf.store.api.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CustomerListResponse {
    List<CustomerResponse> items;
    int page;
    int size;
    long totalItems;
}
