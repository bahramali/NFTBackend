package se.hydroleaf.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.User;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.model.UserStatus;
import se.hydroleaf.repository.UserRepository;
import se.hydroleaf.service.AuthService;
import se.hydroleaf.store.model.OrderItem;
import se.hydroleaf.store.model.OrderStatus;
import se.hydroleaf.store.model.ShippingAddress;
import se.hydroleaf.store.model.StoreOrder;
import se.hydroleaf.store.repository.OrderRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminCustomerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    @Autowired
    private OrderRepository orderRepository;

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void listWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listWithoutPermissionReturnsForbidden() throws Exception {
        String password = "Password12345!";
        createAdmin("no-permission@example.com", password, Set.of());
        String token = authService.login("no-permission@example.com", password).accessToken();

        mockMvc.perform(get("/api/admin/customers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void listWithPermissionReturnsOk() throws Exception {
        String password = "Password12345!";
        createAdmin("permission@example.com", password, Set.of(Permission.CUSTOMERS_VIEW));
        String token = authService.login("permission@example.com", password).accessToken();

        mockMvc.perform(get("/api/admin/customers")
                        .param("sort", "last_order_desc")
                        .param("page", "1")
                        .param("size", "6")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(6))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void listCustomerIdsResolveInDetailsEndpoint() throws Exception {
        String password = "Password12345!";
        createAdmin("permission@example.com", password, Set.of(Permission.CUSTOMERS_VIEW));
        User customer = createCustomer("customer@example.com");
        createOrder("customer@example.com");
        String token = authService.login("permission@example.com", password).accessToken();

        MvcResult result = mockMvc.perform(get("/api/admin/customers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String customerId = JsonPath.read(responseBody, "$.items[0].id");

        mockMvc.perform(get("/api/admin/customers/" + customerId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId))
                .andExpect(jsonPath("$.email").value(customer.getEmail()))
                .andExpect(jsonPath("$.orders").isArray());
    }

    @Test
    void listWithQueryFiltersCustomers() throws Exception {
        String password = "Password12345!";
        createAdmin("permission@example.com", password, Set.of(Permission.CUSTOMERS_VIEW));
        createCustomer("alpha@example.com", "Alpha Customer", LocalDateTime.now());
        createCustomer("beta@example.com", "Beta Customer", LocalDateTime.now().minusDays(120));
        createOrder("guest@example.com", 1000);
        String token = authService.login("permission@example.com", password).accessToken();

        mockMvc.perform(get("/api/admin/customers")
                        .param("q", "alpha")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].email").value("alpha@example.com"));
    }

    @Test
    void listWithStatusAndTypeFiltersCustomers() throws Exception {
        String password = "Password12345!";
        createAdmin("permission@example.com", password, Set.of(Permission.CUSTOMERS_VIEW));
        createCustomer("active@example.com", "Active Customer", LocalDateTime.now());
        createCustomer("inactive@example.com", "Inactive Customer", LocalDateTime.now().minusDays(120));
        createOrder("guest@example.com", 1000);
        String token = authService.login("permission@example.com", password).accessToken();

        mockMvc.perform(get("/api/admin/customers")
                        .param("status", "ACTIVE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2));

        mockMvc.perform(get("/api/admin/customers")
                        .param("type", "GUEST")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].email").value("guest@example.com"));
    }

    @Test
    void listWithCombinedFiltersAndPagination() throws Exception {
        String password = "Password12345!";
        createAdmin("permission@example.com", password, Set.of(Permission.CUSTOMERS_VIEW));
        createCustomer("alpha.one@example.com", "Alpha One", LocalDateTime.now());
        createCustomer("alpha.two@example.com", "Alpha Two", LocalDateTime.now());
        createOrder("guest.one@example.com", 2000);
        createOrder("guest.two@example.com", 1000);
        String token = authService.login("permission@example.com", password).accessToken();

        mockMvc.perform(get("/api/admin/customers")
                        .param("q", "alpha")
                        .param("status", "ACTIVE")
                        .param("type", "REGISTERED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2));

        mockMvc.perform(get("/api/admin/customers")
                        .param("type", "GUEST")
                        .param("sort", "total_spent_desc")
                        .param("page", "2")
                        .param("size", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[0].email").value("guest.two@example.com"));
    }

    @Test
    void detailsUnknownCustomerReturnsNotFound() throws Exception {
        String password = "Password12345!";
        createAdmin("permission@example.com", password, Set.of(Permission.CUSTOMERS_VIEW));
        String token = authService.login("permission@example.com", password).accessToken();

        mockMvc.perform(get("/api/admin/customers/unknown")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private User createAdmin(String email, String password, Set<Permission> permissions) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .displayName("Admin")
                .role(UserRole.ADMIN)
                .permissions(permissions)
                .active(true)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }

    private User createCustomer(String email) {
        return createCustomer(email, "Customer", LocalDateTime.now());
    }

    private User createCustomer(String email, String displayName, LocalDateTime createdAt) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode("Password12345!"))
                .displayName(displayName)
                .role(UserRole.CUSTOMER)
                .permissions(Set.of())
                .active(true)
                .status(UserStatus.ACTIVE)
                .createdAt(createdAt)
                .build();
        return userRepository.save(user);
    }

    private StoreOrder createOrder(String email, long totalCents) {
        StoreOrder order = StoreOrder.builder()
                .orderNumber("ORDER-" + UUID.randomUUID())
                .email(email)
                .status(OrderStatus.PAID)
                .subtotalCents(totalCents)
                .shippingCents(0)
                .taxCents(0)
                .totalCents(totalCents)
                .currency("SEK")
                .shippingAddress(ShippingAddress.builder()
                        .name("Customer")
                        .line1("Main Street 1")
                        .city("Stockholm")
                        .postalCode("11111")
                        .country("SE")
                        .phone("0700000000")
                        .build())
                .build();
        OrderItem item = OrderItem.builder()
                .order(order)
                .nameSnapshot("Sample item")
                .unitPriceCents(totalCents)
                .qty(1)
                .lineTotalCents(totalCents)
                .build();
        order.setItems(java.util.List.of(item));
        return orderRepository.save(order);
    }

    private StoreOrder createOrder(String email) {
        return createOrder(email, 1000);
    }
}
