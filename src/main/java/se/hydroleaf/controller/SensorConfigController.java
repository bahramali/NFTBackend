package se.hydroleaf.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import se.hydroleaf.model.SensorConfig;
import se.hydroleaf.service.AuthorizationService;
import se.hydroleaf.service.SensorConfigService;

import java.util.List;

@RestController
@RequestMapping("/api/sensor-config")
public class SensorConfigController {

    private final SensorConfigService service;
    private final AuthorizationService authorizationService;

    public SensorConfigController(SensorConfigService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<SensorConfig> getAll() {
        return service.getAll();
    }

    @GetMapping("/{sensorType}")
    public SensorConfig get(@PathVariable String sensorType) {
        try {
            return service.get(sensorType);
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, iae.getMessage(), iae);
        }
    }

    @PostMapping
    public SensorConfig create(@RequestHeader(name = "Authorization", required = false) String token,
                               @Valid @RequestBody SensorConfig config) {
        authorizationService.requireMonitoringConfig(token);
        config.setSensorType(config.getSensorType());
        try {
            return service.create(config);
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, iae.getMessage(), iae);
        }
    }

    @PostMapping("/{sensorType}")
    public SensorConfig create(@RequestHeader(name = "Authorization", required = false) String token,
                               @PathVariable String sensorType,
                               @Valid @RequestBody SensorConfig config) {
        authorizationService.requireMonitoringConfig(token);
        config.setSensorType(sensorType);
        try {
            return service.create(config);
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, iae.getMessage(), iae);
        }
    }

    @PutMapping("/{sensorType}")
    public SensorConfig update(@RequestHeader(name = "Authorization", required = false) String token,
                               @PathVariable String sensorType,
                               @Valid @RequestBody SensorConfig config) {
        authorizationService.requireMonitoringConfig(token);
        try {
            return service.update(sensorType, config);
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, iae.getMessage(), iae);
        }
    }

    @DeleteMapping("/{sensorType}")
    public void delete(@RequestHeader(name = "Authorization", required = false) String token,
                       @PathVariable String sensorType) {
        authorizationService.requireMonitoringConfig(token);
        try {
            service.delete(sensorType);
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, iae.getMessage(), iae);
        }
    }
}
