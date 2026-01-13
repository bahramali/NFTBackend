package se.hydroleaf.store.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import se.hydroleaf.store.model.Payment;
import se.hydroleaf.store.model.PaymentProvider;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByProviderPaymentId(String providerPaymentId);

    Optional<Payment> findByOrderIdAndProvider(UUID orderId, PaymentProvider provider);
}
