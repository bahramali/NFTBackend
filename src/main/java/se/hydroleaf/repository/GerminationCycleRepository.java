package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.hydroleaf.model.GerminationCycle;

public interface GerminationCycleRepository extends JpaRepository<GerminationCycle, String> {
}

