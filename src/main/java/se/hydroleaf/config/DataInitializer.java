package se.hydroleaf.config;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.User;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.repository.UserRepository;
import se.hydroleaf.service.AuthService;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final AuthService authService;

    @Bean
    CommandLineRunner seedUsers() {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }
            BCryptPasswordEncoder encoder = authService.passwordEncoder();
            User superAdmin = User.builder()
                    .email("super@admin.com")
                    .password(encoder.encode("password"))
                    .role(UserRole.SUPER_ADMIN)
                    .permissions(Set.of(
                            Permission.VIEW_DASHBOARD,
                            Permission.MANAGE_ORDERS,
                            Permission.MANAGE_PRODUCTS,
                            Permission.MANAGE_USERS))
                    .active(true)
                    .displayName("Super Admin")
                    .build();

            User admin = User.builder()
                    .email("admin@example.com")
                    .password(encoder.encode("password"))
                    .role(UserRole.ADMIN)
                    .permissions(Set.of(Permission.VIEW_DASHBOARD, Permission.MANAGE_ORDERS, Permission.MANAGE_USERS))
                    .active(true)
                    .displayName("Admin User")
                    .build();

            User worker = User.builder()
                    .email("worker@example.com")
                    .password(encoder.encode("password"))
                    .role(UserRole.WORKER)
                    .permissions(Set.of())
                    .active(true)
                    .displayName("Worker User")
                    .build();

            User customer = User.builder()
                    .email("customer@example.com")
                    .password(encoder.encode("password"))
                    .role(UserRole.CUSTOMER)
                    .permissions(Set.of())
                    .active(true)
                    .displayName("Customer User")
                    .build();

            userRepository.save(superAdmin);
            userRepository.save(admin);
            userRepository.save(worker);
            userRepository.save(customer);
        };
    }
}
