package se.hydroleaf.store.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import se.hydroleaf.store.model.StoreOrder;

public interface OrderRepository extends JpaRepository<StoreOrder, UUID> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<StoreOrder> findById(UUID id);

    Optional<StoreOrder> findByOrderNumber(String orderNumber);
}
