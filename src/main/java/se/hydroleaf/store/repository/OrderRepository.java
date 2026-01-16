package se.hydroleaf.store.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.hydroleaf.store.model.StoreOrder;

public interface OrderRepository extends JpaRepository<StoreOrder, UUID> {

    @EntityGraph(attributePaths = {"items", "items.product", "items.variant"})
    Optional<StoreOrder> findById(UUID id);

    Optional<StoreOrder> findByOrderNumber(String orderNumber);

    List<StoreOrder> findByEmailIgnoreCase(String email);

    @Query("""
            select o as order, p as payment, count(i) as itemsCount, coalesce(sum(i.qty), 0) as itemsQuantity
            from StoreOrder o
            left join Payment p
                on p.order = o
                and p.updatedAt = (select max(p2.updatedAt) from Payment p2 where p2.order = o)
            left join o.items i
            where lower(o.email) = lower(:email)
            group by o, p
            """)
    List<OrderSummaryRow> findOrderSummariesByEmail(@Param("email") String email);

    @Query("""
            select o.id as id, count(i) as itemsCount, coalesce(sum(i.qty), 0) as itemsQuantity
            from StoreOrder o
            left join o.items i
            where o.id in :orderIds
            group by o.id
            """)
    List<OrderItemCounts> findItemCounts(@Param("orderIds") List<UUID> orderIds);

    boolean existsByItemsProductId(UUID productId);

    boolean existsByItemsVariantId(UUID variantId);

    boolean existsByItemsVariantProductId(UUID productId);

    interface OrderItemCounts {
        UUID getId();

        long getItemsCount();

        long getItemsQuantity();
    }

    interface OrderSummaryRow {
        StoreOrder getOrder();

        se.hydroleaf.store.model.Payment getPayment();

        long getItemsCount();

        long getItemsQuantity();
    }
}
