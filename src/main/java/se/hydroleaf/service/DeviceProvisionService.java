package se.hydroleaf.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.hydroleaf.model.Device;
import se.hydroleaf.model.DeviceGroup;
import se.hydroleaf.repository.DeviceGroupRepository;
import se.hydroleaf.repository.DeviceRepository;

import java.util.Optional;

@Service
public class DeviceProvisionService {

    private final DeviceRepository deviceRepo;
    private final DeviceGroupRepository groupRepo;

    @Value("${mqtt.topicPrefix:}")
    private String topicPrefix;

    public DeviceProvisionService(DeviceRepository deviceRepo,
                                  DeviceGroupRepository groupRepo) {
        this.deviceRepo = deviceRepo;
        this.groupRepo = groupRepo;
    }

    public Device ensureDevice(String compositeId, String topic) {
        return deviceRepo.findById(compositeId).orElseGet(() -> {
            Device d = new Device();
            d.setCompositeId(compositeId);

            String[] parts = compositeId.split("-");
            if (parts.length >= 3) {
                d.setSystem(parts[0]);
                d.setLayer(parts[1]);
                d.setDeviceId(parts[2]);
            }

            String grpKey = (topic != null && !topic.isBlank()) ? topic.split("/")[0] : topicPrefix;
            DeviceGroup g = findOrCreateGroup(grpKey == null ? "default" : grpKey);
            d.setGroup(g);

            return deviceRepo.save(d);
        });
    }

    private DeviceGroup findOrCreateGroup(String mqttKey) {
        Optional<DeviceGroup> g = groupRepo.findByMqttTopic(mqttKey);
        if (g.isPresent()) return g.get();

        DeviceGroup ng = new DeviceGroup();
        ng.setMqttTopic(mqttKey);
        return groupRepo.save(ng);
    }
}
