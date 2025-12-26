package se.hydroleaf.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.repository.dto.summary.StatusAllAverageResponse;
import se.hydroleaf.repository.dto.summary.StatusAverageResponse;
import se.hydroleaf.service.AuthorizationService;
import se.hydroleaf.service.StatusService;

@RestController
@RequestMapping("/api/status")
public class StatusController {

    private final StatusService statusService;
    private final AuthorizationService authorizationService;

    public StatusController(StatusService statusService, AuthorizationService authorizationService) {
        this.statusService = statusService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/{system}/{layer}/{sensorType}/average")
    public StatusAverageResponse getAverage(
            @RequestHeader(name = "Authorization", required = false) String token,
            @PathVariable String system,
            @PathVariable String layer,
            @PathVariable String sensorType) {
        authorizationService.requireMonitoringView(token);
        return statusService.getAverage(system, layer, sensorType);
    }

    @GetMapping("/{system}/{layer}/all/average")
    public StatusAllAverageResponse getAllAverages(
            @RequestHeader(name = "Authorization", required = false) String token,
            @PathVariable String system,
            @PathVariable String layer) {
        authorizationService.requireMonitoringView(token);
        return statusService.getAllAverages(system, layer);
    }

}
