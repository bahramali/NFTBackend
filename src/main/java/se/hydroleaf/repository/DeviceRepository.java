package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import se.hydroleaf.model.Device;
import se.hydroleaf.model.TopicName;
import se.hydroleaf.repository.dto.SystemLayer;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, String> {

    List<Device> findBySystemAndLayer(String system, String layer);

    List<Device> findByOwnerUserId(Long ownerUserId);

    Optional<Device> findByOwnerUserIdAndDeviceId(Long ownerUserId, String deviceId);

    @Query("SELECT DISTINCT new se.hydroleaf.repository.dto.SystemLayer(d.system, d.layer) FROM Device d")
    List<SystemLayer> findDistinctSystemAndLayer();

    Optional<Device> findFirstByTopic(TopicName topic);
}
