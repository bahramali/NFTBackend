package se.hydroleaf.store.api.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CustomerDetailsResponse {
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
