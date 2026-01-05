package se.hydroleaf.store.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.hydroleaf.store.model.ProductVariant;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    Optional<ProductVariant> findByIdAndActiveTrue(UUID id);

    Optional<ProductVariant> findBySku(String sku);

    Optional<ProductVariant> findByProductIdAndWeightGrams(UUID productId, int weightGrams);

    Optional<ProductVariant> findByIdAndProductId(UUID id, UUID productId);

    List<ProductVariant> findByProductId(UUID productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ProductVariant v join fetch v.product where v.id = :id")
    Optional<ProductVariant> findWithLock(@Param("id") UUID id);
}
