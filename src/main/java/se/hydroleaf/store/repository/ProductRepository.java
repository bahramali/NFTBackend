package se.hydroleaf.store.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.hydroleaf.store.model.Product;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    @EntityGraph(attributePaths = "variants")
    List<Product> findByActiveTrue();

    @EntityGraph(attributePaths = "variants")
    Optional<Product> findByIdAndActiveTrue(UUID id);

    @Override
    @EntityGraph(attributePaths = "variants")
    List<Product> findAll();

    @Override
    @EntityGraph(attributePaths = "variants")
    Optional<Product> findById(UUID id);

    Optional<Product> findBySku(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findWithLock(@Param("id") UUID id);
}
