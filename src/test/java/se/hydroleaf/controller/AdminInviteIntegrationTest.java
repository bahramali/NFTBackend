package se.hydroleaf.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.User;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.model.UserStatus;
import se.hydroleaf.repository.UserRepository;
import se.hydroleaf.service.AuthService;
import se.hydroleaf.service.InviteEmailService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminInviteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private InviteEmailService inviteEmailService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void inviteAcceptAndLoginFlow() throws Exception {
        createUser("super@example.com", "password123", UserRole.SUPER_ADMIN, Set.of(Permission.TEAM));
        String token = bearerToken("super@example.com", "password123");

        mockMvc.perform(post("/api/super-admin/admins/invite")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteRequestJson("invited@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("INVITED"));

        String inviteToken = inviteEmailService.lastTokenFor("invited@example.com").orElseThrow();

        mockMvc.perform(post("/api/auth/accept-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AcceptPayload(inviteToken, "averystrongpass"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        User admin = userRepository.findByEmailIgnoreCase("invited@example.com").orElseThrow();
        assertThat(admin.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(admin.isActive()).isTrue();
        assertThat(admin.isInvited()).isFalse();
        assertThat(admin.getInviteTokenHash()).isNull();
    }

    @Test
    void resendInviteGeneratesNewToken() throws Exception {
        createUser("super@example.com", "password123", UserRole.SUPER_ADMIN, Set.of(Permission.TEAM));
        String token = bearerToken("super@example.com", "password123");

        mockMvc.perform(post("/api/super-admin/admins/invite")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteRequestJson("resend@example.com")))
                .andExpect(status().isCreated());

        User admin = userRepository.findByEmailIgnoreCase("resend@example.com").orElseThrow();
        String firstToken = inviteEmailService.lastTokenFor("resend@example.com").orElseThrow();

        mockMvc.perform(post("/api/super-admin/admins/" + admin.getId() + "/resend-invite")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVITED"));

        String secondToken = inviteEmailService.lastTokenFor("resend@example.com").orElseThrow();
        assertThat(secondToken).isNotEqualTo(firstToken);

        User refreshed = userRepository.findByEmailIgnoreCase("resend@example.com").orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(UserStatus.INVITED);
        assertThat(refreshed.isActive()).isFalse();
    }

    @Test
    void invalidPermissionReturnsReadableError() throws Exception {
        createUser("super@example.com", "password123", UserRole.SUPER_ADMIN, Set.of(Permission.TEAM));
        String token = bearerToken("super@example.com", "password123");

        mockMvc.perform(post("/api/super-admin/admins/invite")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InvalidPermissionInvitePayload(
                                "invited@example.com", Set.of("INVALID_PERMISSION")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("permissions[0]"))
                .andExpect(jsonPath("$.errors[0].message", containsString("Allowed values")));
    }

    @Test
    void unknownFieldIsRejected() throws Exception {
        createUser("super@example.com", "password123", UserRole.SUPER_ADMIN, Set.of(Permission.TEAM));
        String token = bearerToken("super@example.com", "password123");

        String body = "{\"email\":\"invited@example.com\",\"permissions\":[\"ADMIN_DASHBOARD\"],\"unexpected\":true}";

        mockMvc.perform(post("/api/super-admin/admins/invite")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("unexpected"))
                .andExpect(jsonPath("$.errors[0].message").value("Unrecognized field 'unexpected'"));
    }

    @Test
    void validateInviteEndpointReturnsMetadata() throws Exception {
        createUser("super@example.com", "password123", UserRole.SUPER_ADMIN, Set.of(Permission.TEAM));
        String token = bearerToken("super@example.com", "password123");

        mockMvc.perform(post("/api/super-admin/admins/invite")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteRequestJson("invited@example.com", "Invited Admin")))
                .andExpect(status().isCreated());

        String inviteToken = inviteEmailService.lastTokenFor("invited@example.com").orElseThrow();

        mockMvc.perform(get("/api/auth/accept-invite/" + inviteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("invited@example.com"))
                .andExpect(jsonPath("$.displayName").value("Invited Admin"))
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    void validateInviteRejectsExpiredToken() throws Exception {
        createUser("super@example.com", "password123", UserRole.SUPER_ADMIN, Set.of(Permission.TEAM));
        String token = bearerToken("super@example.com", "password123");

        mockMvc.perform(post("/api/super-admin/admins/invite")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteRequestJson("expired@example.com")))
                .andExpect(status().isCreated());

        User invited = userRepository.findByEmailIgnoreCase("expired@example.com").orElseThrow();
        invited.setInviteExpiresAt(LocalDateTime.now().minusHours(1));
        userRepository.save(invited);

        String inviteToken = inviteEmailService.lastTokenFor("expired@example.com").orElseThrow();

        mockMvc.perform(get("/api/auth/accept-invite/" + inviteToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invite has expired"));
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

    private String inviteRequestJson(String email) throws Exception {
        return inviteRequestJson(email, null);
    }

    private String inviteRequestJson(String email, String displayName) throws Exception {
        return objectMapper.writeValueAsString(new InvitePayload(email, displayName, Set.of(Permission.ADMIN_DASHBOARD)));
    }

    private record InvitePayload(String email, String displayName, Set<Permission> permissions) {}

    private record InvalidPermissionInvitePayload(String email, Set<String> permissions) {}

    private record AcceptPayload(String token, String password) {}
}
