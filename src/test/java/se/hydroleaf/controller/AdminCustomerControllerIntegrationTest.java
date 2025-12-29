package se.hydroleaf.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.User;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.model.UserStatus;
import se.hydroleaf.repository.UserRepository;
import se.hydroleaf.service.AuthService;

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

    @AfterEach
    void tearDown() {
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
        String token = authService.login("no-permission@example.com", password).token();

        mockMvc.perform(get("/api/admin/customers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void listWithPermissionReturnsOk() throws Exception {
        String password = "Password12345!";
        createAdmin("permission@example.com", password, Set.of(Permission.CUSTOMERS_VIEW));
        String token = authService.login("permission@example.com", password).token();

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
}
