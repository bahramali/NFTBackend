package se.hydroleaf.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.repository.dto.DeviceResponse;
import se.hydroleaf.repository.dto.DeviceSensorsResponse;
import se.hydroleaf.service.AuthorizationService;
import se.hydroleaf.service.DeviceService;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;
    private final AuthorizationService authorizationService;

    public DeviceController(DeviceService deviceService, AuthorizationService authorizationService) {
        this.deviceService = deviceService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<DeviceResponse> getAllDevices(
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.requireMonitoringView(token);
        return deviceService.getAllDevices();
    }

    @GetMapping("/composite-ids")
    public List<String> getCompositeIds(@RequestHeader(name = "Authorization", required = false) String token,
                                        @RequestParam String system,
                                        @RequestParam String layer,
                                        @RequestParam(required = false) String deviceId) {
        authorizationService.requireMonitoringView(token);
        return deviceService.getCompositeIds(system, layer, deviceId);
    }

    @GetMapping("/all")
    public DeviceSensorsResponse getAllDevicesWithSensors(
            @RequestHeader(name = "Authorization", required = false) String token) {
        authorizationService.requireMonitoringView(token);
        return deviceService.getAllDevicesWithSensors();
    }

    @GetMapping("/sensors")
    public DeviceSensorsResponse getSensorsForDevices(
            @RequestHeader(name = "Authorization", required = false) String token,
            @RequestParam List<String> compositeIds) {
        authorizationService.requireMonitoringView(token);
        try {
            return deviceService.getSensorsForDevices(compositeIds);
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage(), iae);
        }
    }
}
