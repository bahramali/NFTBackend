package se.hydroleaf.store.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import se.hydroleaf.common.api.NotFoundException;
import se.hydroleaf.model.User;
import se.hydroleaf.repository.UserRepository;
import se.hydroleaf.store.api.dto.CustomerDetailsResponse;
import se.hydroleaf.store.api.dto.CustomerOrderSummaryResponse;
import se.hydroleaf.store.api.dto.CustomerResponse;
import se.hydroleaf.store.model.ShippingAddress;
import se.hydroleaf.store.model.StoreOrder;
import se.hydroleaf.store.repository.OrderRepository;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private static final String USER_PREFIX = "user_";
    private static final String GUEST_PREFIX = "guest_";

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public List<CustomerResponse> listCustomers() {
        List<StoreOrder> orders = orderRepository.findAll();
        Map<String, List<StoreOrder>> ordersByEmail = orders.stream()
                .filter(order -> order.getEmail() != null)
                .collect(Collectors.groupingBy(order -> normalizeEmail(order.getEmail())));
        return ordersByEmail.entrySet().stream()
                .map(entry -> buildCustomerResponse(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CustomerResponse::getLastOrderAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public CustomerDetailsResponse getCustomerDetails(String customerId) {
        CustomerIdentity identity = parseIdentity(customerId);
        if (identity.userId().isPresent()) {
            User user = userRepository.findById(identity.userId().get())
                    .orElseThrow(() -> new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found"));
            return buildCustomerDetails(user.getEmail(), user);
        }
        return buildCustomerDetails(identity.email(), null);
    }

    private CustomerDetailsResponse buildCustomerDetails(String email, User user) {
        List<StoreOrder> orders = orderRepository.findByEmailIgnoreCase(email);
        if (orders.isEmpty()) {
            throw new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found");
        }
        StoreOrder latestOrder = orders.stream()
                .max(Comparator.comparing(StoreOrder::getCreatedAt))
                .orElse(null);
        String name = resolveName(user, latestOrder);
        String phone = resolvePhone(user, latestOrder);
        CustomerDetailsResponse.Profile profile = CustomerDetailsResponse.Profile.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .build();
        List<CustomerOrderSummaryResponse> summaries = orders.stream()
                .sorted(Comparator.comparing(StoreOrder::getCreatedAt).reversed())
                .map(order -> CustomerOrderSummaryResponse.builder()
                        .id(order.getId())
                        .date(toLocalDate(order.getCreatedAt()))
                        .total(order.getTotalCents())
                        .status(order.getStatus().name())
                        .build())
                .toList();
        return CustomerDetailsResponse.builder()
                .profile(profile)
                .orders(summaries)
                .build();
    }

    private CustomerResponse buildCustomerResponse(String email, List<StoreOrder> orders) {
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        StoreOrder latestOrder = orders.stream()
                .max(Comparator.comparing(StoreOrder::getCreatedAt))
                .orElse(null);
        String name = resolveName(user, latestOrder);
        String customerType = user != null ? "REGISTERED" : "GUEST";
        int ordersCount = orders.size();
        long totalSpent = orders.stream().mapToLong(StoreOrder::getTotalCents).sum();
        LocalDate lastOrderAt = latestOrder != null ? toLocalDate(latestOrder.getCreatedAt()) : null;
        String status = user != null ? user.getStatus().name() : "GUEST";
        return CustomerResponse.builder()
                .id(user != null ? encodeUserId(user.getId()) : encodeGuestEmail(email))
                .name(name)
                .email(email)
                .customerType(customerType)
                .ordersCount(ordersCount)
                .totalSpent(totalSpent)
                .lastOrderAt(lastOrderAt)
                .status(status)
                .build();
    }

    private String resolveName(User user, StoreOrder latestOrder) {
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName();
        }
        ShippingAddress address = latestOrder != null ? latestOrder.getShippingAddress() : null;
        if (address != null && address.getName() != null && !address.getName().isBlank()) {
            return address.getName();
        }
        return user != null ? user.getEmail() : null;
    }

    private String resolvePhone(User user, StoreOrder latestOrder) {
        if (user != null && user.getPhone() != null && !user.getPhone().isBlank()) {
            return user.getPhone();
        }
        ShippingAddress address = latestOrder != null ? latestOrder.getShippingAddress() : null;
        if (address != null && address.getPhone() != null && !address.getPhone().isBlank()) {
            return address.getPhone();
        }
        return null;
    }

    private LocalDate toLocalDate(java.time.Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(ZoneOffset.UTC).toLocalDate();
    }

    private String encodeUserId(Long userId) {
        return USER_PREFIX + userId;
    }

    private String encodeGuestEmail(String email) {
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(normalizeEmail(email).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return GUEST_PREFIX + encoded;
    }

    private CustomerIdentity parseIdentity(String id) {
        if (id == null) {
            throw new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found");
        }
        if (id.startsWith(USER_PREFIX)) {
            String raw = id.substring(USER_PREFIX.length());
            try {
                return new CustomerIdentity(Optional.of(Long.parseLong(raw)), null);
            } catch (NumberFormatException ex) {
                throw new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found");
            }
        }
        if (id.startsWith(GUEST_PREFIX)) {
            String raw = id.substring(GUEST_PREFIX.length());
            try {
                byte[] decoded = Base64.getUrlDecoder().decode(raw);
                String email = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
                return new CustomerIdentity(Optional.empty(), normalizeEmail(email));
            } catch (IllegalArgumentException ex) {
                throw new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found");
            }
        }
        throw new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found");
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private record CustomerIdentity(Optional<Long> userId, String email) {}
}
