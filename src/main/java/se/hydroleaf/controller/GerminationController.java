package se.hydroleaf.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping
    public GerminationStatusResponse getStatus() {
        return germinationService.getStatus()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Germination start time not found"));
    }

    @PostMapping("/start")
    public GerminationStatusResponse triggerStart() {
        return germinationService.triggerStart();
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public GerminationStatusResponse updateStart(@Valid @RequestBody GerminationStartRequest request) {
        return germinationService.updateStart(request.startTime());
    }
}

