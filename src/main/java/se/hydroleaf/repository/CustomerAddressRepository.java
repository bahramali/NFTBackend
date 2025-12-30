package se.hydroleaf.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.hydroleaf.model.CustomerAddress;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {

    List<CustomerAddress> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<CustomerAddress> findByIdAndUserId(Long id, Long userId);

    Optional<CustomerAddress> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);

    @Modifying
    @Query("update CustomerAddress a set a.isDefault = false where a.user.id = :userId")
    void clearDefaultForUser(@Param("userId") Long userId);

    @Modifying
    @Query("""
            update CustomerAddress a
            set a.isDefault = false
            where a.user.id = :userId
              and a.id <> :addressId
            """)
    void clearDefaultForUser(@Param("userId") Long userId, @Param("addressId") Long addressId);
}
