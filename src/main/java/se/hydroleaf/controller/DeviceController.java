package se.hydroleaf.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.repository.dto.DeviceResponse;
import se.hydroleaf.repository.dto.DeviceSensorsResponse;
import se.hydroleaf.service.DeviceService;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public List<DeviceResponse> getAllDevices() {
        return deviceService.getAllDevices();
    }

    @GetMapping("/all")
    public DeviceSensorsResponse getAllDevicesWithSensors() {
        return deviceService.getAllDevicesWithSensors();
    }

    @GetMapping("/sensors")
    public DeviceSensorsResponse getSensorsForDevices(@RequestParam List<String> compositeIds) {
        try {
            return deviceService.getSensorsForDevices(compositeIds);
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage(), iae);
        }
    }
}
