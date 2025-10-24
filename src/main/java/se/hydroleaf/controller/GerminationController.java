package se.hydroleaf.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.controller.dto.GerminationStartRequest;
import se.hydroleaf.repository.dto.GerminationStatusResponse;
import se.hydroleaf.service.GerminationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/germination")
public class GerminationController {

    private final GerminationService germinationService;

    public GerminationController(GerminationService germinationService) {
        this.germinationService = germinationService;
    }

    @GetMapping("/{system}/{layer}/{deviceId}")
    public GerminationStatusResponse getStatus(@PathVariable String system,
                                               @PathVariable String layer,
                                               @PathVariable String deviceId) {
        String compositeId = buildCompositeId(system, layer, deviceId);
        return germinationService.getStatus(compositeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Germination start time not found for composite_id: " + compositeId));
    }

    @PostMapping("/{system}/{layer}/{deviceId}/start")
    public GerminationStatusResponse triggerStart(@PathVariable String system,
                                                  @PathVariable String layer,
                                                  @PathVariable String deviceId) {
        return germinationService.triggerStart(buildCompositeId(system, layer, deviceId));
    }

    @PutMapping("/{system}/{layer}/{deviceId}")
    @ResponseStatus(HttpStatus.OK)
    public GerminationStatusResponse updateStart(@PathVariable String system,
                                                 @PathVariable String layer,
                                                 @PathVariable String deviceId,
                                                 @Valid @RequestBody GerminationStartRequest request) {
        return germinationService.updateStart(buildCompositeId(system, layer, deviceId), request.startTime());
    }

    private static String buildCompositeId(String system, String layer, String deviceId) {
        return String.join("-", system, layer, deviceId);
    }
}

