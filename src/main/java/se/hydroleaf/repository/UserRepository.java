package se.hydroleaf.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.hydroleaf.model.User;
import se.hydroleaf.model.UserRole;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByRole(UserRole role);

    Optional<User> findByInviteTokenHash(String inviteTokenHash);

    List<User> findAllByRole(UserRole role);
}
