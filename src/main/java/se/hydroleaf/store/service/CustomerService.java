package se.hydroleaf.store.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.hydroleaf.common.api.NotFoundException;
import se.hydroleaf.model.User;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.repository.UserRepository;
import se.hydroleaf.store.api.dto.CustomerDetailsResponse;
import se.hydroleaf.store.api.dto.CustomerListResponse;
import se.hydroleaf.store.api.dto.CustomerOrderSummaryResponse;
import se.hydroleaf.store.api.dto.CustomerResponse;
import se.hydroleaf.store.model.ShippingAddress;
import se.hydroleaf.store.model.OrderStatus;
import se.hydroleaf.store.model.StoreOrder;
import se.hydroleaf.store.repository.OrderRepository;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private static final String USER_PREFIX = "user_";
    private static final String GUEST_PREFIX = "guest_";
    private static final int ACTIVE_DAYS = 90;

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public CustomerListResponse listCustomers(String query, String status, String type, String sort, int page, int size) {
        List<CustomerAggregate> aggregates = loadAggregates();
        Predicate<CustomerAggregate> filter = buildFilter(query, status, type);
        List<CustomerResponse> filtered = aggregates.stream()
                .filter(filter)
                .map(this::buildCustomerResponse)
                .sorted(resolveSort(sort))
                .toList();
        int safeSize = size > 0 ? size : 20;
        int safePage = Math.max(page, 1);
        int pageIndex = safePage - 1;
        int totalItems = filtered.size();
        int fromIndex = Math.min(pageIndex * safeSize, totalItems);
        int toIndex = Math.min(fromIndex + safeSize, totalItems);
        List<CustomerResponse> pageItems = filtered.subList(fromIndex, toIndex);
        return CustomerListResponse.builder()
                .items(pageItems)
                .page(safePage)
                .size(safeSize)
                .totalItems(totalItems)
                .build();
    }

    @Transactional(readOnly = true)
    public CustomerDetailsResponse getCustomerDetails(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found");
        }
        if (customerId.startsWith(USER_PREFIX) || customerId.startsWith(GUEST_PREFIX)) {
            CustomerIdentity identity = parseIdentity(customerId);
            if (identity.userId().isPresent()) {
                User user = userRepository.findById(identity.userId().get())
                        .orElseThrow(() -> new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found"));
                return buildCustomerDetails(user.getEmail(), user);
            }
            return buildCustomerDetails(identity.email(), null);
        }
        Optional<User> userById = resolveUserById(customerId);
        if (userById.isPresent()) {
            User user = userById.get();
            return buildCustomerDetails(user.getEmail(), user);
        }
        Optional<String> emailFromOrder = resolveEmailFromOrderId(customerId);
        if (emailFromOrder.isPresent()) {
            return buildCustomerDetails(emailFromOrder.get(), null);
        }
        if (customerId.contains("@")) {
            return buildCustomerDetails(customerId, null);
        }
        throw new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found");
    }

    private CustomerDetailsResponse buildCustomerDetails(String email, User user) {
        String normalizedEmail = normalizeEmail(email);
        List<StoreOrder> orders = normalizedEmail != null
                ? orderRepository.findByEmailIgnoreCase(normalizedEmail)
                : List.of();
        if (orders.isEmpty() && user == null) {
            throw new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found");
        }
        StoreOrder latestOrder = orders.stream()
                .max(Comparator.comparing(StoreOrder::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        String name = resolveName(user, latestOrder);
        String phone = resolvePhone(user, latestOrder);
        CustomerDetailsResponse.Profile profile = CustomerDetailsResponse.Profile.builder()
                .name(name)
                .email(normalizedEmail)
                .phone(phone)
                .build();
        long totalSpent = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID)
                .mapToLong(StoreOrder::getTotalCents)
                .sum();
        Instant lastOrderAt = latestOrder != null ? latestOrder.getCreatedAt() : null;
        String currency = latestOrder != null ? latestOrder.getCurrency() : null;
        Instant createdAt = user != null ? toInstant(user.getCreatedAt()) : null;
        Instant lastLoginAt = user != null ? toInstant(user.getLastLoginAt()) : null;
        Instant lastSeenAt = resolveLastSeen(createdAt, lastLoginAt, lastOrderAt);
        List<CustomerOrderSummaryResponse> summaries = orders.stream()
                .sorted(Comparator.comparing(StoreOrder::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .map(order -> CustomerOrderSummaryResponse.builder()
                        .orderId(order.getId())
                        .createdAt(order.getCreatedAt())
                        .total(order.getTotalCents())
                        .currency(order.getCurrency())
                        .status(order.getStatus().name())
                        .itemsCount(order.getItems() != null ? order.getItems().size() : 0)
                        .itemsQuantity(order.getItems() != null
                                ? order.getItems().stream().mapToInt(item -> item.getQty()).sum()
                                : 0)
                        .build())
                .toList();
        String customerType = user != null ? "REGISTERED" : "GUEST";
        String resolvedStatus = resolveStatus(user, latestOrder);
        return CustomerDetailsResponse.builder()
                .id(user != null ? encodeUserId(user.getId()) : encodeGuestEmail(normalizedEmail))
                .name(name)
                .email(normalizedEmail)
                .customerType(customerType)
                .status(resolvedStatus)
                .createdAt(createdAt)
                .lastLoginAt(lastLoginAt)
                .lastSeenAt(lastSeenAt)
                .totalSpent(totalSpent)
                .currency(currency)
                .lastOrderAt(lastOrderAt)
                .profile(profile)
                .orders(summaries)
                .build();
    }

    private CustomerResponse buildCustomerResponse(CustomerAggregate aggregate) {
        User user = aggregate.user();
        List<StoreOrder> orders = aggregate.orders();
        StoreOrder latestOrder = orders.stream()
                .max(Comparator.comparing(StoreOrder::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        String name = resolveName(user, latestOrder);
        String customerType = user != null ? "REGISTERED" : "GUEST";
        int ordersCount = orders.size();
        long totalSpent = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID)
                .mapToLong(StoreOrder::getTotalCents)
                .sum();
        Instant lastOrderAt = latestOrder != null ? latestOrder.getCreatedAt() : null;
        String resolvedStatus = resolveStatus(user, latestOrder);
        String currency = latestOrder != null ? latestOrder.getCurrency() : null;
        return CustomerResponse.builder()
                .id(user != null ? encodeUserId(user.getId()) : encodeGuestEmail(aggregate.email()))
                .name(name)
                .email(aggregate.email())
                .customerType(customerType)
                .ordersCount(ordersCount)
                .totalSpent(totalSpent)
                .currency(currency)
                .lastOrderAt(lastOrderAt)
                .status(resolvedStatus)
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

    private Optional<User> resolveUserById(String customerId) {
        try {
            long id = Long.parseLong(customerId);
            return userRepository.findById(id);
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private Optional<String> resolveEmailFromOrderId(String customerId) {
        try {
            java.util.UUID orderId = java.util.UUID.fromString(customerId);
            return orderRepository.findById(orderId)
                    .map(StoreOrder::getEmail);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private Instant resolveLastSeen(Instant createdAt, Instant lastLoginAt, Instant lastOrderAt) {
        Instant lastSeen = createdAt;
        if (lastLoginAt != null && (lastSeen == null || lastLoginAt.isAfter(lastSeen))) {
            lastSeen = lastLoginAt;
        }
        if (lastOrderAt != null && (lastSeen == null || lastOrderAt.isAfter(lastSeen))) {
            lastSeen = lastOrderAt;
        }
        return lastSeen;
    }

    private List<CustomerAggregate> loadAggregates() {
        java.util.Map<String, CustomerAggregate> aggregates = new java.util.HashMap<>();
        List<User> users = userRepository.findAllByRole(UserRole.CUSTOMER);
        for (User user : users) {
            String email = normalizeEmail(user.getEmail());
            if (email == null) {
                continue;
            }
            aggregates.computeIfAbsent(email, key -> new CustomerAggregate(key, null, new ArrayList<>()))
                    .setUser(user);
        }
        List<StoreOrder> orders = orderRepository.findAll();
        for (StoreOrder order : orders) {
            if (order.getEmail() == null) {
                continue;
            }
            String email = normalizeEmail(order.getEmail());
            if (email == null) {
                continue;
            }
            aggregates.computeIfAbsent(email, key -> new CustomerAggregate(key, null, new ArrayList<>()))
                    .orders()
                    .add(order);
        }
        return new ArrayList<>(aggregates.values());
    }

    private Predicate<CustomerAggregate> buildFilter(String query, String status, String type) {
        String normalizedQuery = query != null ? query.trim().toLowerCase(Locale.ROOT) : null;
        String normalizedStatus = status != null ? status.trim().toUpperCase(Locale.ROOT) : null;
        String normalizedType = type != null ? type.trim().toUpperCase(Locale.ROOT) : null;
        return aggregate -> {
            CustomerResponse response = buildCustomerResponse(aggregate);
            if (normalizedQuery != null && !normalizedQuery.isBlank()) {
                String name = response.getName() != null ? response.getName().toLowerCase(Locale.ROOT) : "";
                String email = response.getEmail() != null ? response.getEmail().toLowerCase(Locale.ROOT) : "";
                if (!name.contains(normalizedQuery) && !email.contains(normalizedQuery)) {
                    return false;
                }
            }
            if (normalizedStatus != null && !normalizedStatus.isBlank()) {
                if (!normalizedStatus.equalsIgnoreCase(response.getStatus())) {
                    return false;
                }
            }
            if (normalizedType != null && !normalizedType.isBlank()) {
                if (!normalizedType.equalsIgnoreCase(response.getCustomerType())) {
                    return false;
                }
            }
            return true;
        };
    }

    private Comparator<CustomerResponse> resolveSort(String sort) {
        String normalizedSort = sort != null ? sort.trim().toUpperCase(Locale.ROOT) : "LAST_ORDER_DESC";
        return switch (normalizedSort) {
            case "TOTAL_SPENT_DESC" -> Comparator.comparingLong(CustomerResponse::getTotalSpent).reversed();
            case "ORDERS_COUNT_DESC" -> Comparator.comparingInt(CustomerResponse::getOrdersCount).reversed();
            case "LAST_ORDER_DESC" -> Comparator.comparing(CustomerResponse::getLastOrderAt,
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed();
            default -> Comparator.comparing(CustomerResponse::getLastOrderAt,
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        };
    }

    private String resolveStatus(User user, StoreOrder latestOrder) {
        Instant cutoff = Instant.now().minus(ACTIVE_DAYS, ChronoUnit.DAYS);
        if (latestOrder != null && latestOrder.getCreatedAt() != null && latestOrder.getCreatedAt().isAfter(cutoff)) {
            return "ACTIVE";
        }
        if (user != null) {
            Instant lastLogin = toInstant(user.getLastLoginAt());
            if (lastLogin != null && lastLogin.isAfter(cutoff)) {
                return "ACTIVE";
            }
            Instant created = toInstant(user.getCreatedAt());
            if (created != null && created.isAfter(cutoff)) {
                return "ACTIVE";
            }
        }
        return "INACTIVE";
    }

    private Instant toInstant(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.toInstant(ZoneOffset.UTC);
    }

    private record CustomerIdentity(Optional<Long> userId, String email) {}

    private static class CustomerAggregate {
        private final String email;
        private User user;
        private final List<StoreOrder> orders;

        private CustomerAggregate(String email, User user, List<StoreOrder> orders) {
            this.email = email;
            this.user = user;
            this.orders = orders;
        }

        private String email() {
            return email;
        }

        private User user() {
            return user;
        }

        private List<StoreOrder> orders() {
            return orders;
        }

        private void setUser(User user) {
            this.user = user;
        }
    }
}
