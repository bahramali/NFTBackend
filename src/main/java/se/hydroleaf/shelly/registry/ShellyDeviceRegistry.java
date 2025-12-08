package se.hydroleaf.shelly.registry;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import se.hydroleaf.shelly.model.ShellyDeviceConfig;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ShellyDeviceRegistry {

    private final Map<String, ShellyDeviceConfig> devicesById = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        registerDevice(ShellyDeviceConfig.builder()
                .id("PS01L02")
                .name("Shelly Plug 02")
                .ip("192.168.8.45")
                .build());
        registerDevice(ShellyDeviceConfig.builder()
                .id("PS01L03")
                .name("Shelly Plug 03")
                .ip("192.168.8.46")
                .build());
        registerDevice(ShellyDeviceConfig.builder()
                .id("PS01L04")
                .name("Shelly Plug 04")
                .ip("192.168.8.47")
                .build());
    }

    public void registerDevice(ShellyDeviceConfig device) {
        devicesById.put(device.getId(), device);
    }

    public Collection<ShellyDeviceConfig> getAllDevices() {
        return Collections.unmodifiableCollection(devicesById.values());
    }

    public Optional<ShellyDeviceConfig> getDevice(String id) {
        return Optional.ofNullable(devicesById.get(id));
    }
}
