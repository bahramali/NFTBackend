package se.hydroleaf.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import se.hydroleaf.model.SensorConfig;

public interface SensorConfigRepository extends JpaRepository<SensorConfig, Long> {

    Optional<SensorConfig> findBySensorType(String sensorType);

    boolean existsBySensorType(String sensorType);

    void deleteBySensorType(String sensorType);
}

