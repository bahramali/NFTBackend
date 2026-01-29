package se.hydroleaf.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.repository.dto.DeviceResponse;
import se.hydroleaf.service.AuthorizationService;
import se.hydroleaf.service.DeviceService;
import se.hydroleaf.shelly.model.Rack;
import se.hydroleaf.shelly.registry.ShellyRegistry;

@RestController
@RequestMapping("/api")
public class RackController {

    private final ShellyRegistry registry;
    private final DeviceService deviceService;
    private final AuthorizationService authorizationService;

    public RackController(ShellyRegistry registry,
                          DeviceService deviceService,
                          AuthorizationService authorizationService) {
        this.registry = registry;
        this.deviceService = deviceService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/racks")
    public List<Rack> getRacks() {
        return registry.getRacks();
    }

    @GetMapping("/racks/{rackId}/nodes")
    public List<DeviceResponse> getRackNodes(
            @RequestHeader(name = "Authorization", required = false) String token,
            @PathVariable String rackId) {
        authorizationService.requireMonitoringView(token);
        return deviceService.getDevicesByRack(rackId);
    }
}
