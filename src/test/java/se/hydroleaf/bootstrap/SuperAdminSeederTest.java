package se.hydroleaf.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
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
    private SuperAdminSeeder superAdminSeeder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void seedsSuperAdminFromConfigWhenMissing() throws Exception {
        superAdminSeeder.run(new DefaultApplicationArguments(new String[]{}));

        User superAdmin = userRepository.findByEmailIgnoreCase("seed@example.com").orElseThrow();
        assertThat(superAdmin.getRole()).isEqualTo(UserRole.SUPER_ADMIN);
        assertThat(superAdmin.getDisplayName()).isEqualTo("Super Admin");
        assertThat(superAdmin.isActive()).isTrue();
        assertThat(superAdmin.getPermissions()).isEmpty();
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(authService.passwordEncoder().matches("averystrongpassword", superAdmin.getPassword())).isTrue();
    }

    @Test
    void doesNothingWhenSuperAdminAlreadyExists() throws Exception {
        User existing = User.builder()
                .email("seed@example.com")
                .password(authService.passwordEncoder().encode("averystrongpassword"))
                .role(UserRole.SUPER_ADMIN)
                .active(true)
                .build();
        userRepository.save(existing);

        superAdminSeeder.run(new DefaultApplicationArguments(new String[]{}));

        long superAdmins = userRepository.findAll().stream()
                .filter(user -> user.getRole() == UserRole.SUPER_ADMIN)
                .count();
        assertThat(superAdmins).isEqualTo(1);
    }
}
