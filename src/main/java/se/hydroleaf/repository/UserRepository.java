package se.hydroleaf.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.hydroleaf.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
