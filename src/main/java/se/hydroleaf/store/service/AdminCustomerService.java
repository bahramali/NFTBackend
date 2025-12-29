package se.hydroleaf.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import se.hydroleaf.store.api.dto.CustomerDetailsResponse;
import se.hydroleaf.store.api.dto.CustomerListResponse;
import se.hydroleaf.store.api.dto.CustomersPageResponse;

@Service
@RequiredArgsConstructor
public class AdminCustomerService {

    private final CustomerService customerService;

    public CustomersPageResponse list(String q, String status, String type, String sort, int page, int size) {
        CustomerListResponse response = customerService.listCustomers(q, status, type, sort, page, size);
        return CustomersPageResponse.builder()
                .items(response.getItems())
                .page(response.getPage())
                .size(response.getSize())
                .totalItems(response.getTotalItems())
                .totalElements(response.getTotalElements())
                .totalPages(response.getTotalPages())
                .build();
    }

    public CustomerDetailsResponse getCustomerDetails(String customerId) {
        return customerService.getCustomerDetails(customerId);
    }
}
