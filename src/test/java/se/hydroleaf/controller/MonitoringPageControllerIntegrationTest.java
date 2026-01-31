package se.hydroleaf.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import se.hydroleaf.model.MonitoringPage;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.User;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.model.UserStatus;
import se.hydroleaf.repository.MonitoringPageRepository;
import se.hydroleaf.repository.UserRepository;
import se.hydroleaf.service.AuthService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MonitoringPageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MonitoringPageRepository monitoringPageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    @AfterEach
    void tearDown() {
        monitoringPageRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void publicListReturnsEnabledPagesInOrder() throws Exception {
        String token = loginAdmin(Set.of(Permission.MONITORING_VIEW));
        monitoringPageRepository.save(createPage("rack-a", "Alpha", "rack-alpha", 1, true));
        monitoringPageRepository.save(createPage("rack-b", "Beta", "rack-beta", 0, true));
        monitoringPageRepository.save(createPage("rack-c", "Disabled", "rack-disabled", 2, false));

        mockMvc.perform(get("/api/monitoring-pages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].rackId").value("rack-b"))
                .andExpect(jsonPath("$[1].rackId").value("rack-a"));
    }

    @Test
    void publicGetDisabledPageReturnsNotFound() throws Exception {
        String token = loginAdmin(Set.of(Permission.MONITORING_VIEW));
        monitoringPageRepository.save(createPage("rack-a", "Alpha", "rack-alpha", 0, false));

        mockMvc.perform(get("/api/monitoring-pages/rack-alpha")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicGetPageFallsBackTelemetryRackIdForLegacyRackIds() throws Exception {
        String token = loginAdmin(Set.of(Permission.MONITORING_VIEW));
        monitoringPageRepository.save(createPage("RACK_01", "Legacy Rack", "legacy-rack", 0, true));

        mockMvc.perform(get("/api/monitoring-pages/legacy-rack")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telemetryRackId").value("R01"));
    }

    @Test
    void publicGetPageFallsBackTelemetryRackIdToRackIdWhenNoMatch() throws Exception {
        String token = loginAdmin(Set.of(Permission.MONITORING_VIEW));
        monitoringPageRepository.save(createPage("rack-alpha", "Alpha", "rack-alpha", 0, true));

        mockMvc.perform(get("/api/monitoring-pages/rack-alpha")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telemetryRackId").value("rack-alpha"));
    }

    @Test
    void publicGetPageUsesTelemetryRackIdWhenProvided() throws Exception {
        String token = loginAdmin(Set.of(Permission.MONITORING_VIEW));
        monitoringPageRepository.save(createPage("RACK_01", "Legacy Rack", "legacy-rack", 0, true, "R99"));

        mockMvc.perform(get("/api/monitoring-pages/legacy-rack")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telemetryRackId").value("R99"));
    }

    @Test
    void adminCreateDuplicateRackIdReturnsConflict() throws Exception {
        String token = loginAdmin(Set.of(Permission.MONITORING_CONFIG));
        monitoringPageRepository.save(createPage("rack-a", "Alpha", "rack-alpha", 0, true));

        mockMvc.perform(post("/api/admin/monitoring-pages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rackId": "rack-a",
                                  "telemetryRackId": "R1",
                                  "title": "Another",
                                  "slug": "rack-another",
                                  "sortOrder": 1,
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void adminCreatePageStoresTelemetryRackId() throws Exception {
        String token = loginAdmin(Set.of(Permission.MONITORING_CONFIG));

        mockMvc.perform(post("/api/admin/monitoring-pages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rackId": "rack-a",
                                  "telemetryRackId": "R03",
                                  "title": "Alpha",
                                  "slug": "rack-alpha",
                                  "sortOrder": 1,
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telemetryRackId").value("R03"));

        MonitoringPage savedPage = monitoringPageRepository.findAll().get(0);
        assertThat(savedPage.getTelemetryRackId()).isEqualTo("R03");
    }

    @Test
    void adminUpdatePageUpdatesTelemetryRackId() throws Exception {
        String token = loginAdmin(Set.of(Permission.MONITORING_CONFIG));
        MonitoringPage page = monitoringPageRepository.save(
                createPage("rack-a", "Alpha", "rack-alpha", 0, true, "R01"));

        mockMvc.perform(put("/api/admin/monitoring-pages/{id}", page.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Alpha Updated",
                                  "telemetryRackId": "R05",
                                  "slug": "rack-alpha",
                                  "sortOrder": 2,
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telemetryRackId").value("R05"));

        MonitoringPage savedPage = monitoringPageRepository.findById(page.getId()).orElseThrow();
        assertThat(savedPage.getTelemetryRackId()).isEqualTo("R05");
    }

    @Test
    void adminCreateDuplicateSlugReturnsConflict() throws Exception {
        String token = loginAdmin(Set.of(Permission.MONITORING_CONFIG));
        monitoringPageRepository.save(createPage("rack-a", "Alpha", "rack-alpha", 0, true));

        mockMvc.perform(post("/api/admin/monitoring-pages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rackId": "rack-b",
                                  "telemetryRackId": "R2",
                                  "title": "Beta",
                                  "slug": "rack-alpha",
                                  "sortOrder": 1,
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isConflict());
    }

    private String loginAdmin(Set<Permission> permissions) {
        String email = "admin-" + System.nanoTime() + "@example.com";
        String password = "Password12345!";
        createAdmin(email, password, permissions);
        return authService.login(email, password).accessToken();
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

    private MonitoringPage createPage(String rackId, String title, String slug, int sortOrder, boolean enabled) {
        return createPage(rackId, title, slug, sortOrder, enabled, null);
    }

    private MonitoringPage createPage(
            String rackId,
            String title,
            String slug,
            int sortOrder,
            boolean enabled,
            String telemetryRackId) {
        return MonitoringPage.builder()
                .rackId(rackId)
                .telemetryRackId(telemetryRackId)
                .title(title)
                .slug(slug)
                .sortOrder(sortOrder)
                .enabled(enabled)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
