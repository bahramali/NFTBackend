package se.hydroleaf.controller;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.service.AuthenticatedUser;
import se.hydroleaf.service.AuthorizationService;

@RestController
@RequestMapping("/api/worker")
@RequiredArgsConstructor
public class WorkerController {

    private final AuthorizationService authorizationService;

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(@RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser user = authorizationService.requireAuthenticated(token);
        authorizationService.requireRole(user, UserRole.WORKER);
        return Map.of("tasks", 3, "message", "Worker dashboard data");
    }
}
