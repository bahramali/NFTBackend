package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import se.hydroleaf.model.Device;
import se.hydroleaf.model.TopicName;
import se.hydroleaf.repository.dto.SystemLayer;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, String> {

    @Query("SELECT d FROM Device d WHERE d.system = :system AND d.rack = :telemetryRackId AND d.layer = :layer")
    List<Device> findBySystemAndTelemetryRackIdAndLayer(String system, String telemetryRackId, String layer);

    @Query("SELECT d FROM Device d WHERE d.rack = :telemetryRackId")
    List<Device> findByTelemetryRackId(String telemetryRackId);

    List<Device> findByOwnerUserId(Long ownerUserId);

    Optional<Device> findByOwnerUserIdAndDeviceId(Long ownerUserId, String deviceId);

    @Query("SELECT DISTINCT new se.hydroleaf.repository.dto.SystemLayer(d.system, d.layer) FROM Device d")
    List<SystemLayer> findDistinctSystemAndLayer();

    Optional<Device> findFirstByTopic(TopicName topic);
}
