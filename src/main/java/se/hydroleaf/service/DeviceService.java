package se.hydroleaf.service;

import org.springframework.stereotype.Service;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.LatestSensorValueRepository;
import se.hydroleaf.repository.dto.DeviceResponse;
import se.hydroleaf.repository.dto.DeviceSensorsResponse;
import se.hydroleaf.model.Device;
import se.hydroleaf.model.LatestSensorValue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final LatestSensorValueRepository latestSensorValueRepository;

    public DeviceService(DeviceRepository deviceRepository,
                         LatestSensorValueRepository latestSensorValueRepository) {
        this.deviceRepository = deviceRepository;
        this.latestSensorValueRepository = latestSensorValueRepository;
    }

    public List<DeviceResponse> getAllDevices() {
        return deviceRepository.findAll().stream()
                .map(d -> new DeviceResponse(d.getCompositeId(), d.getSystem(), d.getLayer(), d.getDeviceId()))
                .toList();
    }

    public DeviceSensorsResponse getSensorsForDevices(List<String> compositeIds) {
        List<Device> devices = deviceRepository.findAllById(compositeIds);
        Set<String> found = devices.stream().map(Device::getCompositeId).collect(Collectors.toSet());
        List<String> missing = compositeIds.stream()
                .filter(id -> !found.contains(id))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Unknown device composite_id(s): " + String.join(",", missing));
        }

        Map<String, Set<String>> sensorsByDevice = latestSensorValueRepository
                .findByDevice_CompositeIdIn(compositeIds).stream()
                .collect(Collectors.groupingBy(lsv -> lsv.getDevice().getCompositeId(),
                        Collectors.mapping(LatestSensorValue::getSensorType, Collectors.toSet())));

        List<DeviceSensorsResponse.DeviceInfo> deviceInfos = devices.stream()
                .map(d -> new DeviceSensorsResponse.DeviceInfo(
                        d.getSystem(),
                        d.getLayer(),
                        d.getDeviceId(),
                        sensorsByDevice.getOrDefault(d.getCompositeId(), Set.of()).stream().toList()))
                .toList();

        List<DeviceSensorsResponse.SystemInfo> systemInfos = devices.stream()
                .collect(Collectors.groupingBy(Device::getSystem,
                        Collectors.mapping(Device::getLayer, Collectors.toSet())))
                .entrySet().stream()
                .map(e -> new DeviceSensorsResponse.SystemInfo(e.getKey(), e.getValue().stream().toList()))
                .toList();

        String version = Instant.now().toString();

        return new DeviceSensorsResponse(version, systemInfos, deviceInfos);
    }
}
