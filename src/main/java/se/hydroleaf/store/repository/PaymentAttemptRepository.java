package se.hydroleaf.store.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import se.hydroleaf.store.model.PaymentAttempt;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {
    Optional<PaymentAttempt> findByStripeSessionId(String stripeSessionId);
}
