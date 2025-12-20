package se.hydroleaf.store.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import se.hydroleaf.store.model.StoreOrder;

public interface OrderRepository extends JpaRepository<StoreOrder, UUID> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<StoreOrder> findById(UUID id);

    Optional<StoreOrder> findByOrderNumber(String orderNumber);

    List<StoreOrder> findByEmailIgnoreCase(String email);

    boolean existsByItemsProductId(UUID productId);
}
