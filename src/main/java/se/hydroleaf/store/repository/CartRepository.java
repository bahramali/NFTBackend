package se.hydroleaf.store.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.hydroleaf.store.model.Cart;
import se.hydroleaf.store.model.CartStatus;

import jakarta.persistence.LockModeType;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    @EntityGraph(attributePaths = "items")
    Optional<Cart> findById(UUID id);

    @EntityGraph(attributePaths = "items")
    Optional<Cart> findBySessionIdAndStatus(String sessionId, CartStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cart c left join fetch c.items where c.id = :id")
    Optional<Cart> findLockedWithItems(@Param("id") UUID id);
}
