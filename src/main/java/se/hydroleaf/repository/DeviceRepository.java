package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.hydroleaf.model.Device;

import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, String> {

    List<Device> findBySystemAndLayer(String system, String layer);
}
