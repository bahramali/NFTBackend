package se.hydroleaf.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.User;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.repository.UserRepository;
import se.hydroleaf.service.AuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "classpath:schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SuperAdminSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void adminCannotCreateSuperAdmin() throws Exception {
        createUser("admin@example.com", "password123", UserRole.ADMIN, Set.of(Permission.MANAGE_USERS));

        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearerToken("admin@example.com", "password123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserRequestJson("root@example.com", "SUPER_ADMIN")))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findAll()).noneMatch(user -> user.getRole() == UserRole.SUPER_ADMIN);
    }

    @Test
    void adminCannotPromoteUserToSuperAdmin() throws Exception {
        createUser("admin@example.com", "password123", UserRole.ADMIN, Set.of(Permission.MANAGE_USERS));
        User worker = createUser("worker@example.com", "password123", UserRole.WORKER, Set.of());

        mockMvc.perform(put("/api/users/" + worker.getId())
                        .header("Authorization", bearerToken("admin@example.com", "password123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateUserRequestJson("SUPER_ADMIN")))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findById(worker.getId())).get().extracting(User::getRole).isEqualTo(UserRole.WORKER);
    }

    @Test
    void superAdminManagesAdminsThroughDedicatedEndpoints() throws Exception {
        createUser("super@example.com", "password123", UserRole.SUPER_ADMIN, Set.of(Permission.MANAGE_USERS));
        String token = bearerToken("super@example.com", "password123");

        mockMvc.perform(post("/api/super-admin/admins/invite")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createAdminInviteRequest("new.admin@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("INVITED"));

        User createdAdmin = userRepository.findByEmailIgnoreCase("new.admin@example.com").orElseThrow();

        mockMvc.perform(get("/api/super-admin/admins")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("new.admin@example.com"));

        mockMvc.perform(put("/api/super-admin/admins/" + createdAdmin.getId() + "/permissions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePermissionsRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions[0]").value("MANAGE_USERS"));

        mockMvc.perform(put("/api/super-admin/admins/" + createdAdmin.getId() + "/status")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusUpdateJson(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));

        mockMvc.perform(delete("/api/super-admin/admins/" + createdAdmin.getId())
                        .header("Authorization", token))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(createdAdmin.getId())).isEmpty();
    }

    @Test
    void unauthorizedRolesCannotUseSuperAdminEndpoints() throws Exception {
        createUser("worker@example.com", "password123", UserRole.WORKER, Set.of());
        createUser("admin@example.com", "password123", UserRole.ADMIN, Set.of(Permission.MANAGE_USERS));

        mockMvc.perform(get("/api/super-admin/admins"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/super-admin/admins")
                        .header("Authorization", bearerToken("worker@example.com", "password123")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/super-admin/admins")
                        .header("Authorization", bearerToken("admin@example.com", "password123")))
                .andExpect(status().isForbidden());
    }

    private User createUser(String email, String password, UserRole role, Set<Permission> permissions) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .permissions(permissions)
                .active(true)
                .build();
        return userRepository.save(user);
    }

    private String bearerToken(String email, String password) {
        AuthService.LoginResult result = authService.login(email, password);
        return "Bearer " + result.token();
    }

    private String createUserRequestJson(String email, String role) throws Exception {
        return objectMapper.writeValueAsString(new UserRequestPayload(email, role, "password123"));
    }

    private String updateUserRequestJson(String role) throws Exception {
        return objectMapper.writeValueAsString(new UserUpdatePayload(role));
    }

    private String createAdminInviteRequest(String email) throws Exception {
        return objectMapper.writeValueAsString(new AdminInvitePayload(email));
    }

    private String updatePermissionsRequestJson() throws Exception {
        return objectMapper.writeValueAsString(new PermissionPayload(Set.of("MANAGE_USERS")));
    }

    private String statusUpdateJson(boolean active) throws Exception {
        return objectMapper.writeValueAsString(new StatusPayload(active));
    }

    private record UserRequestPayload(String email, String role, String password) {}

    private record UserUpdatePayload(String role) {}

    private record AdminInvitePayload(String email) {}

    private record PermissionPayload(Set<String> permissions) {}

    private record StatusPayload(boolean active) {}
}
