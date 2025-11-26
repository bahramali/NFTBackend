package se.hydroleaf.controller;

import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.controller.dto.LedCommandRequest;
import se.hydroleaf.controller.dto.LedCommandResponse;
import se.hydroleaf.controller.dto.LedScheduleRequest;
import se.hydroleaf.service.ActuatorCommandService;

@RestController
@RequestMapping("/api/actuators")
@ConditionalOnProperty(prefix = "mqtt", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ActuatorController {

    private final ActuatorCommandService actuatorCommandService;

    public ActuatorController(ActuatorCommandService actuatorCommandService) {
        this.actuatorCommandService = actuatorCommandService;
    }

    @PostMapping("/led/command")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public LedCommandResponse sendLedCommand(@Valid @RequestBody LedCommandRequest request) {
        return actuatorCommandService.publishLedCommand(request);
    }

    @PostMapping("/led/schedule")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public LedCommandResponse setLedSchedule(@Valid @RequestBody LedScheduleRequest request) {
        return actuatorCommandService.publishLedSchedule(request);
    }
}
