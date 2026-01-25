package se.hydroleaf.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import se.hydroleaf.controller.dto.UserCreateRequest;
import se.hydroleaf.model.Device;
import se.hydroleaf.model.LatestSensorValue;
import se.hydroleaf.model.TopicName;
import se.hydroleaf.model.User;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.repository.CustomerAddressRepository;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.LatestSensorValueRepository;
import se.hydroleaf.repository.UserRepository;
import se.hydroleaf.service.AuthService;
import se.hydroleaf.service.UserService;
import se.hydroleaf.store.model.OrderStatus;
import se.hydroleaf.store.model.ShippingAddress;
import se.hydroleaf.store.model.StoreOrder;
import se.hydroleaf.store.repository.OrderRepository;
import se.hydroleaf.store.repository.PaymentRepository;

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
    private PaymentRepository paymentRepository;

    @Autowired
    private CustomerAddressRepository customerAddressRepository;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void cleanDatabase() {
        latestSensorValueRepository.deleteAll();
        deviceRepository.deleteAll();
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        customerAddressRepository.deleteAll();
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
    void customerCanUpdateProfileWithPut() throws Exception {
        User user = createCustomer("update@example.com", "Customer One");
        String token = bearerToken(user.getEmail(), "password123");

        mockMvc.perform(put("/api/me")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Updated Name",
                                  "phoneNumber": "+46 70 123 45 67",
                                  "orderConfirmationEmails": false,
                                  "pickupReadyNotification": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.phone").value("+46 70 123 45 67"))
                .andExpect(jsonPath("$.phoneNumber").value("+46 70 123 45 67"))
                .andExpect(jsonPath("$.orderConfirmationEmails").value(false))
                .andExpect(jsonPath("$.pickupReadyNotification").value(true))
                .andExpect(jsonPath("$.notificationPreferences.orderConfirmationEmails").value(false))
                .andExpect(jsonPath("$.notificationPreferences.pickupReadyNotification").value(true));

        mockMvc.perform(get("/api/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.phone").value("+46 70 123 45 67"));
    }

    @Test
    void customerCanUpdateProfileWithTrailingSlash() throws Exception {
        User user = createCustomer("update2@example.com", "Customer Two");
        String token = bearerToken(user.getEmail(), "password123");

        mockMvc.perform(put("/api/me/")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Trailing Slash",
                                  "phoneNumber": "+46 70 999 11 22",
                                  "orderConfirmationEmails": true,
                                  "pickupReadyNotification": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Trailing Slash"))
                .andExpect(jsonPath("$.phoneNumber").value("+46 70 999 11 22"));
    }

    @Test
    void customerAccessingDeviceEndpointsIsForbidden() throws Exception {
        User customer = createCustomer("owner@example.com", "Owner");
        Device owned = saveDevice("SYS-R01-L1-D1", "SYS", "R01", "L1", "D1", customer.getId());
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
                .andExpect(jsonPath("$[0].totalAmountCents").value(1500));

        mockMvc.perform(get("/api/store/orders/" + myOrder.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(myOrder.getId().toString()))
                .andExpect(jsonPath("$.orderNumber").value("HL-100"))
                .andExpect(jsonPath("$.orderStatus").value(OrderStatus.PROCESSING.name()));
    }

    @Test
    void accessingOtherOrderReturns403() throws Exception {
        User owner = createCustomer("owner3@example.com", "Owner Three");
        User other = createCustomer("other3@example.com", "Other Three");

        StoreOrder othersOrder = saveOrder("HL-300", other.getEmail(), 3300);

        String token = bearerToken(owner.getEmail(), "password123");

        mockMvc.perform(get("/api/store/orders/" + othersOrder.getId()).header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void addressEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/me/addresses"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/me/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label": "Home",
                                  "fullName": "Ada Lovelace",
                                  "street1": "Street 1",
                                  "postalCode": "12345",
                                  "city": "Stockholm",
                                  "countryCode": "SE"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCanManageAddressesAndDefaults() throws Exception {
        User user = createCustomer("addr@example.com", "Address Owner");
        String token = bearerToken(user.getEmail(), "password123");

        mockMvc.perform(post("/api/me/addresses")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label": "Home",
                                  "fullName": "Ada Lovelace",
                                  "street1": "Street 1",
                                  "postalCode": "12345",
                                  "city": "Stockholm",
                                  "countryCode": "SE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(true))
                .andExpect(jsonPath("$.label").value("Home"))
                .andExpect(jsonPath("$.street1").value("Street 1"));

        String secondResponse = mockMvc.perform(post("/api/me/addresses")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label": "Office",
                                  "fullName": "Second Address",
                                  "street1": "Street 2",
                                  "postalCode": "98765",
                                  "city": "Gothenburg",
                                  "countryCode": "SE"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondId = extractId(secondResponse);

        mockMvc.perform(get("/api/me/addresses").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].isDefault").value(true))
                .andExpect(jsonPath("$[0].isDefault").value(false))
                .andExpect(jsonPath("$[0].label").value("Office"))
                .andExpect(jsonPath("$[1].label").value("Home"));

        mockMvc.perform(put("/api/me/addresses/" + secondId + "/default")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(true));

        mockMvc.perform(get("/api/me/addresses").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isDefault").value(true))
                .andExpect(jsonPath("$[1].isDefault").value(false));

        mockMvc.perform(put("/api/me/addresses/" + secondId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label": "Billing",
                                  "fullName": "Updated Name",
                                  "street1": "Street 2",
                                  "street2": "Unit 5",
                                  "postalCode": "98765",
                                  "city": "Gothenburg",
                                  "countryCode": "SE",
                                  "phoneNumber": "+46 70 100 10 10",
                                  "isDefault": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Billing"))
                .andExpect(jsonPath("$.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.street2").value("Unit 5"))
                .andExpect(jsonPath("$.phoneNumber").value("+46 70 100 10 10"))
                .andExpect(jsonPath("$.isDefault").value(true));

        mockMvc.perform(delete("/api/me/addresses/" + secondId).header("Authorization", token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/me/addresses").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].isDefault").value(true))
                .andExpect(jsonPath("$[0].label").value("Home"));
    }

    @Test
    void customerCannotAccessOthersAddresses() throws Exception {
        User owner = createCustomer("owner-address@example.com", "Owner");
        User other = createCustomer("other-address@example.com", "Other");
        String ownerToken = bearerToken(owner.getEmail(), "password123");
        String otherToken = bearerToken(other.getEmail(), "password123");

        String response = mockMvc.perform(post("/api/me/addresses")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label": "Home",
                                  "fullName": "Owner Address",
                                  "street1": "Street 1",
                                  "postalCode": "11111",
                                  "city": "Stockholm",
                                  "countryCode": "SE"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String addressId = extractId(response);

        mockMvc.perform(put("/api/me/addresses/" + addressId)
                        .header("Authorization", otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label": "Other",
                                  "fullName": "Other",
                                  "street1": "Street 2",
                                  "postalCode": "22222",
                                  "city": "Gothenburg",
                                  "countryCode": "SE"
                                }
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/me/addresses/" + addressId)
                        .header("Authorization", otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/me/addresses/" + addressId + "/default")
                        .header("Authorization", otherToken))
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

    private Device saveDevice(String compositeId, String system, String rack, String layer, String deviceId, Long ownerId) {
        Device device = Device.builder()
                .compositeId(compositeId)
                .system(system)
                .rack(rack)
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
                .status(OrderStatus.PROCESSING)
                .subtotalCents(totalCents)
                .shippingCents(0)
                .taxCents(0)
                .totalCents(totalCents)
                .totalAmountCents(totalCents)
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

    private String extractId(String responseBody) {
        int idIndex = responseBody.indexOf("\"id\":");
        int start = responseBody.indexOf(':', idIndex) + 1;
        int end = responseBody.indexOf(',', start);
        return responseBody.substring(start, end).trim();
    }

    private String bearerToken(String email, String password) {
        AuthService.LoginResult result = authService.login(email, password);
        return "Bearer " + result.accessToken();
    }
}
