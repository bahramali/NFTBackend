package se.hydroleaf.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import se.hydroleaf.model.User;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.repository.UserRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class SuperAdminSeeder implements ApplicationRunner {

    private static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${APP_SUPERADMIN_EMAIL:}")
    private String superAdminEmail;

    @Value("${APP_SUPERADMIN_PASSWORD:}")
    private String superAdminPassword;

    @Value("${APP_SUPERADMIN_DISPLAY_NAME:Super Admin}")
    private String superAdminDisplayName;

    @Value("${APP_SUPERADMIN_ACTIVE:true}")
    private boolean superAdminActive;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(UserRole.SUPER_ADMIN)) {
            return;
        }

        if (superAdminEmail == null || superAdminEmail.trim().isEmpty()) {
            log.warn("Skipping SUPER_ADMIN seeding: APP_SUPERADMIN_EMAIL is not configured");
            return;
        }
        if (superAdminPassword == null || superAdminPassword.isBlank()) {
            log.warn("Skipping SUPER_ADMIN seeding: APP_SUPERADMIN_PASSWORD is not configured");
            return;
        }
        if (superAdminPassword.length() < MIN_PASSWORD_LENGTH) {
            log.warn("Skipping SUPER_ADMIN seeding: provided password is shorter than {} characters", MIN_PASSWORD_LENGTH);
            return;
        }

        String normalizedEmail = superAdminEmail.trim().toLowerCase();
        String displayName = (superAdminDisplayName == null || superAdminDisplayName.isBlank())
                ? "Super Admin"
                : superAdminDisplayName.trim();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            log.warn("Skipping SUPER_ADMIN seeding: a user with email {} already exists", normalizedEmail);
            return;
        }

        User superAdmin = User.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(superAdminPassword))
                .role(UserRole.SUPER_ADMIN)
                .displayName(displayName)
                .active(superAdminActive)
                .build();

        userRepository.save(superAdmin);
        log.info("Seeded initial SUPER_ADMIN account with email {}", normalizedEmail);
    }
}
