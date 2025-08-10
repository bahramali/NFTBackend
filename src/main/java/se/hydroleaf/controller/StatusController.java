package se.hydroleaf.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.dto.StatusAllAverageResponse;
import se.hydroleaf.dto.StatusAverageResponse;
import se.hydroleaf.service.StatusService;

@RestController
@RequestMapping("/api/status")
public class StatusController {

    private final StatusService statusService;

    public StatusController(StatusService statusService) {
        this.statusService = statusService;
    }

    @GetMapping("/{system}/{layer}/{sensorType}/average")
    public StatusAverageResponse getAverage(
            @PathVariable String system,
            @PathVariable String layer,
            @PathVariable String sensorType) {
        return statusService.getAverage(system, layer, sensorType);
    }

    @GetMapping("/{system}/{layer}/all/average")
    public StatusAllAverageResponse getAllAverages(
            @PathVariable String system,
            @PathVariable String layer) {
        return statusService.getAllAverages(system, layer);
    }
}
