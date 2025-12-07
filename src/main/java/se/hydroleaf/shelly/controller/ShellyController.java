package se.hydroleaf.shelly.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.shelly.dto.ShellyScheduleRequest;
import se.hydroleaf.shelly.dto.ShellyScheduleResponse;
import se.hydroleaf.shelly.dto.ShellyStatusResponse;
import se.hydroleaf.shelly.exception.ShellyException;
import se.hydroleaf.shelly.model.ShellyDeviceConfig;
import se.hydroleaf.shelly.registry.ShellyDeviceRegistry;
import se.hydroleaf.shelly.service.ShellyClientService;
import se.hydroleaf.shelly.service.ShellyScheduleService;

import java.util.Collection;

@RestController
@RequestMapping("/api/shelly")
public class ShellyController {

    private final ShellyClientService clientService;
    private final ShellyScheduleService scheduleService;
    private final ShellyDeviceRegistry deviceRegistry;

    public ShellyController(ShellyClientService clientService, ShellyScheduleService scheduleService, ShellyDeviceRegistry deviceRegistry) {
        this.clientService = clientService;
        this.scheduleService = scheduleService;
        this.deviceRegistry = deviceRegistry;
    }

    @GetMapping("/devices")
    public Collection<ShellyDeviceConfig> getDevices() {
        return deviceRegistry.getAllDevices();
    }

    @GetMapping("/devices/{deviceId}/status")
    public ShellyStatusResponse getStatus(@PathVariable String deviceId) {
        ShellyDeviceConfig device = resolveDevice(deviceId);
        try {
            boolean output = clientService.getStatus(device);
            return new ShellyStatusResponse(deviceId, output);
        } catch (ShellyException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage(), e);
        }
    }

    @PostMapping("/devices/{deviceId}/on")
    public ShellyStatusResponse turnOn(@PathVariable String deviceId) {
        ShellyDeviceConfig device = resolveDevice(deviceId);
        try {
            clientService.turnOn(device);
            return new ShellyStatusResponse(deviceId, true);
        } catch (ShellyException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage(), e);
        }
    }

    @PostMapping("/devices/{deviceId}/off")
    public ShellyStatusResponse turnOff(@PathVariable String deviceId) {
        ShellyDeviceConfig device = resolveDevice(deviceId);
        try {
            clientService.turnOff(device);
            return new ShellyStatusResponse(deviceId, false);
        } catch (ShellyException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage(), e);
        }
    }

    @PostMapping("/devices/{deviceId}/toggle")
    public ShellyStatusResponse toggle(@PathVariable String deviceId) {
        ShellyDeviceConfig device = resolveDevice(deviceId);
        try {
            boolean output = clientService.toggle(device);
            return new ShellyStatusResponse(deviceId, output);
        } catch (ShellyException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage(), e);
        }
    }

    @PostMapping("/devices/{deviceId}/schedule")
    public ShellyScheduleResponse schedule(@PathVariable String deviceId, @RequestBody ShellyScheduleRequest request) {
        try {
            ShellyDeviceConfig device = resolveDevice(deviceId);
            return scheduleService.scheduleDevice(device, request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    private ShellyDeviceConfig resolveDevice(String deviceId) {
        return deviceRegistry.getDevice(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown device id: " + deviceId));
    }
}
