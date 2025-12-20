package se.hydroleaf.controller;

import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.controller.dto.LedCommandRequest;
import se.hydroleaf.controller.dto.LedCommandResponse;
import se.hydroleaf.controller.dto.LedScheduleRequest;
import se.hydroleaf.service.AuthorizationService;
import se.hydroleaf.service.ActuatorCommandService;

@RestController
@RequestMapping("/api/actuators")
@ConditionalOnProperty(prefix = "mqtt", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ActuatorController {

    private final ActuatorCommandService actuatorCommandService;
    private final AuthorizationService authorizationService;

    public ActuatorController(ActuatorCommandService actuatorCommandService,
                              AuthorizationService authorizationService) {
        this.actuatorCommandService = actuatorCommandService;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/led/command")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public LedCommandResponse sendLedCommand(
            @RequestHeader(name = "Authorization", required = false) String token,
            @Valid @RequestBody LedCommandRequest request) {
        authorizationService.requireAdminOrOperator(token);
        return actuatorCommandService.publishLedCommand(request);
    }

    @PostMapping("/led/schedule")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public LedCommandResponse setLedSchedule(
            @RequestHeader(name = "Authorization", required = false) String token,
            @Valid @RequestBody LedScheduleRequest request) {
        authorizationService.requireAdminOrOperator(token);
        return actuatorCommandService.publishLedSchedule(request);
    }
}
