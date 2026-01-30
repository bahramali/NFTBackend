package se.hydroleaf.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.hydroleaf.model.MonitoringPage;

public interface MonitoringPageRepository extends JpaRepository<MonitoringPage, Long> {
    List<MonitoringPage> findAllByEnabledTrueOrderBySortOrderAscTitleAsc();

    List<MonitoringPage> findAllByOrderBySortOrderAscTitleAsc();

    Optional<MonitoringPage> findBySlugAndEnabledTrue(String slug);

    boolean existsByRackId(String rackId);

    boolean existsBySlug(String slug);

    boolean existsByRackIdAndIdNot(String rackId, Long id);

    boolean existsBySlugAndIdNot(String slug, Long id);
}
