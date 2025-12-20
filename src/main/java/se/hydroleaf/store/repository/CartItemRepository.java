package se.hydroleaf.store.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import se.hydroleaf.store.model.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    Optional<CartItem> findByIdAndCartId(UUID id, UUID cartId);

    boolean existsByProductId(UUID productId);
}
