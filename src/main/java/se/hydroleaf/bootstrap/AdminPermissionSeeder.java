package se.hydroleaf.bootstrap;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.User;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.repository.UserRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminPermissionSeeder implements ApplicationRunner {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<User> admins = userRepository.findAllByRole(UserRole.ADMIN);
        if (admins.isEmpty()) {
            return;
        }
        List<User> updated = new ArrayList<>();
        for (User admin : admins) {
            Set<Permission> permissions = admin.getPermissions() == null
                    ? EnumSet.noneOf(Permission.class)
                    : EnumSet.copyOf(admin.getPermissions());
            if (!permissions.contains(Permission.CUSTOMERS_VIEW)) {
                permissions.add(Permission.CUSTOMERS_VIEW);
                admin.setPermissions(permissions);
                updated.add(admin);
            }
        }
        if (!updated.isEmpty()) {
            userRepository.saveAll(updated);
            log.info("Granted {} admins default permission {}", updated.size(), Permission.CUSTOMERS_VIEW);
        }
    }
}
