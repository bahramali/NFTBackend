package se.hydroleaf.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.model.SensorConfig;
import se.hydroleaf.service.SensorConfigService;

import java.util.List;

@RestController
@RequestMapping("/api/sensor-config")
public class SensorConfigController {

    private final SensorConfigService service;

    public SensorConfigController(SensorConfigService service) {
        this.service = service;
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
    public SensorConfig create(@RequestBody SensorConfig config) {
        try {
            return service.create(config);
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, iae.getMessage(), iae);
        }
    }

    @PutMapping("/{sensorType}")
    public SensorConfig update(@PathVariable String sensorType, @RequestBody SensorConfig config) {
        try {
            return service.update(sensorType, config);
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, iae.getMessage(), iae);
        }
    }

    @DeleteMapping("/{sensorType}")
    public void delete(@PathVariable String sensorType) {
        try {
            service.delete(sensorType);
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, iae.getMessage(), iae);
        }
    }
}

