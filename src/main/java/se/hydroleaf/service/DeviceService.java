package se.hydroleaf.service;

import org.springframework.stereotype.Service;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.dto.DeviceResponse;

import java.util.List;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public List<DeviceResponse> getAllDevices() {
        return deviceRepository.findAll().stream()
                .map(d -> new DeviceResponse(d.getCompositeId(), d.getSystem(), d.getLayer(), d.getDeviceId()))
                .toList();
    }
}
