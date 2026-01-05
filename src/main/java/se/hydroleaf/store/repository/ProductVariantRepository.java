package se.hydroleaf.store.repository;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.hydroleaf.store.model.ProductVariant;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    Optional<ProductVariant> findByIdAndActiveTrue(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ProductVariant v join fetch v.product where v.id = :id")
    Optional<ProductVariant> findWithLock(@Param("id") UUID id);
}
