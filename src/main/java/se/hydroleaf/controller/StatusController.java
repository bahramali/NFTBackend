package se.hydroleaf.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.dto.StatusAverageResponse;
import se.hydroleaf.service.StatusService;

@RestController
@RequestMapping("/api/status")
public class StatusController {

    private final StatusService statusService;

    public StatusController(StatusService statusService) {
        this.statusService = statusService;
    }

    @GetMapping("/average")
    public StatusAverageResponse getAverage(
            @RequestParam String system,
            @RequestParam String layer,
            @RequestParam String sensorType) {
        return statusService.getAverage(system, layer, sensorType);
    }
}
