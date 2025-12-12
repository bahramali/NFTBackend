package se.hydroleaf.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.hydroleaf.model.User;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.repository.UserRepository;
import se.hydroleaf.service.AuthService;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.sql.init.mode=never",
        "APP_SUPERADMIN_EMAIL=seed@example.com",
        "APP_SUPERADMIN_PASSWORD=averystrongpassword"
})
@ActiveProfiles("test")
class SuperAdminSeederTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void seedsSuperAdminFromConfigWhenMissing() throws Exception {
        dataInitializer.seedUsers().run();

        User superAdmin = userRepository.findByEmailIgnoreCase("seed@example.com").orElseThrow();
        assertThat(superAdmin.getRole()).isEqualTo(UserRole.SUPER_ADMIN);
        assertThat(superAdmin.getDisplayName()).isEqualTo("Super Admin");
        assertThat(superAdmin.isActive()).isTrue();
        assertThat(superAdmin.getPermissions()).isEqualTo(Set.of());
        assertThat(authService.passwordEncoder().matches("averystrongpassword", superAdmin.getPassword())).isTrue();
    }

    @Test
    void doesNothingWhenSuperAdminAlreadyExists() throws Exception {
        User existing = User.builder()
                .email("seed@example.com")
                .password(authService.passwordEncoder().encode("averystrongpassword"))
                .role(UserRole.SUPER_ADMIN)
                .permissions(Set.of())
                .active(true)
                .build();
        userRepository.save(existing);

        dataInitializer.seedUsers().run();

        long superAdmins = userRepository.findAll().stream()
                .filter(user -> user.getRole() == UserRole.SUPER_ADMIN)
                .count();
        assertThat(superAdmins).isEqualTo(1);
    }
}
