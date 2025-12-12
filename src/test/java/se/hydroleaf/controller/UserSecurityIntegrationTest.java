package se.hydroleaf.controller;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.controller.dto.UserCreateRequest;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.repository.UserRepository;
import se.hydroleaf.service.AuthService;
import se.hydroleaf.service.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserSecurityIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void emailUniquenessIsCaseInsensitive() {
        userService.create(new UserCreateRequest(
                "User@Example.com",
                "password123",
                "User One",
                UserRole.WORKER,
                null,
                Set.of()
        ));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                userService.create(new UserCreateRequest(
                        "user@example.com",
                        "password123",
                        "User Two",
                        UserRole.CUSTOMER,
                        null,
                        Set.of()
                ))
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void inactiveUserCannotLogin() {
        userService.create(new UserCreateRequest(
                "inactive@example.com",
                "password123",
                "Inactive",
                UserRole.CUSTOMER,
                false,
                Set.of()
        ));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                authService.login("inactive@example.com", "password123")
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void nonAdminCannotAccessUserList() throws Exception {
        userService.create(new UserCreateRequest(
                "customer@example.com",
                "password123",
                "Customer",
                UserRole.CUSTOMER,
                null,
                Set.of()
        ));

        String token = bearerToken("customer@example.com", "password123");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminWithoutManageUsersPermissionCannotAccessUserList() throws Exception {
        userService.create(new UserCreateRequest(
                "admin.nopriv@example.com",
                "password123",
                "Admin",
                UserRole.ADMIN,
                null,
                Set.of(Permission.VIEW_DASHBOARD)
        ));

        String token = bearerToken("admin.nopriv@example.com", "password123");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    private String bearerToken(String email, String password) {
        AuthService.LoginResult result = authService.login(email, password);
        return "Bearer " + result.token();
    }
}

