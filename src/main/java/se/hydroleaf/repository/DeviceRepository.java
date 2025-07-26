package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.hydroleaf.model.Device;

public interface DeviceRepository extends JpaRepository<Device, String> {
}
