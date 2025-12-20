package se.hydroleaf.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import se.hydroleaf.controller.dto.UserCreateRequest;
import se.hydroleaf.model.Device;
import se.hydroleaf.model.LatestSensorValue;
import se.hydroleaf.model.TopicName;
import se.hydroleaf.model.User;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.LatestSensorValueRepository;
import se.hydroleaf.repository.UserRepository;
import se.hydroleaf.service.AuthService;
import se.hydroleaf.service.UserService;
import se.hydroleaf.store.model.OrderStatus;
import se.hydroleaf.store.model.ShippingAddress;
import se.hydroleaf.store.model.StoreOrder;
import se.hydroleaf.store.repository.OrderRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MyAccountIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private LatestSensorValueRepository latestSensorValueRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void cleanDatabase() {
        latestSensorValueRepository.deleteAll();
        deviceRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void customerCanFetchProfile() throws Exception {
        User user = createCustomer("me@example.com", "Customer One");
        String token = bearerToken(user.getEmail(), "password123");

        mockMvc.perform(get("/api/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.role").value(UserRole.CUSTOMER.name()));
    }

    @Test
    void customerAccessingDeviceEndpointsIsForbidden() throws Exception {
        User customer = createCustomer("owner@example.com", "Owner");
        Device owned = saveDevice("SYS-L1-D1", "SYS", "L1", "D1", customer.getId());
        saveSensorSnapshot(owned, "temperature", 20.5, Instant.now());

        String token = bearerToken(customer.getEmail(), "password123");

        mockMvc.perform(get("/api/my/devices").header("Authorization", token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/my/devices/D1").header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerSeesOnlyOwnOrders() throws Exception {
        User owner = createCustomer("orders@example.com", "Orders");
        User other = createCustomer("otherorders@example.com", "Other Orders");

        StoreOrder myOrder = saveOrder("HL-100", owner.getEmail(), 1500);
        saveOrder("HL-200", other.getEmail(), 2500);

        String token = bearerToken(owner.getEmail(), "password123");

        mockMvc.perform(get("/api/store/orders/my").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].orderNumber").value("HL-100"))
                .andExpect(jsonPath("$[0].totalCents").value(1500));

        mockMvc.perform(get("/api/store/orders/" + myOrder.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(myOrder.getId().toString()))
                .andExpect(jsonPath("$.orderNumber").value("HL-100"))
                .andExpect(jsonPath("$.status").value(OrderStatus.PAID.name()));
    }

    @Test
    void accessingOtherOrderReturns404() throws Exception {
        User owner = createCustomer("owner3@example.com", "Owner Three");
        User other = createCustomer("other3@example.com", "Other Three");

        StoreOrder othersOrder = saveOrder("HL-300", other.getEmail(), 3300);

        String token = bearerToken(owner.getEmail(), "password123");

        mockMvc.perform(get("/api/store/orders/" + othersOrder.getId()).header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    private User createCustomer(String email, String displayName) {
        return userService.create(new UserCreateRequest(
                email,
                "password123",
                displayName,
                UserRole.CUSTOMER,
                true,
                Set.of()
        ));
    }

    private Device saveDevice(String compositeId, String system, String layer, String deviceId, Long ownerId) {
        Device device = Device.builder()
                .compositeId(compositeId)
                .system(system)
                .layer(layer)
                .deviceId(deviceId)
                .ownerUserId(ownerId)
                .topic(TopicName.growSensors)
                .build();
        return deviceRepository.save(device);
    }

    private void saveSensorSnapshot(Device device, String type, double value, Instant valueTime) {
        LatestSensorValue lsv = LatestSensorValue.builder()
                .device(device)
                .sensorType(type)
                .value(value)
                .valueTime(valueTime)
                .build();
        latestSensorValueRepository.saveAll(List.of(lsv));
    }

    private StoreOrder saveOrder(String orderNumber, String email, long totalCents) {
        StoreOrder order = StoreOrder.builder()
                .orderNumber(orderNumber)
                .email(email)
                .status(OrderStatus.PAID)
                .subtotalCents(totalCents)
                .shippingCents(0)
                .taxCents(0)
                .totalCents(totalCents)
                .currency("SEK")
                .shippingAddress(ShippingAddress.builder()
                        .name("Tester")
                        .line1("123 Test St")
                        .city("Testville")
                        .postalCode("12345")
                        .country("SE")
                        .build())
                .build();
        return orderRepository.save(order);
    }

    private String bearerToken(String email, String password) {
        AuthService.LoginResult result = authService.login(email, password);
        return "Bearer " + result.token();
    }
}
