package se.hydroleaf.controller;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.service.AuthService;
import se.hydroleaf.service.AuthenticatedUser;
import se.hydroleaf.service.AuthorizationService;

@RestController
@RequestMapping("/api/worker")
@RequiredArgsConstructor
public class WorkerController {

    private final AuthService authService;
    private final AuthorizationService authorizationService;

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(@RequestHeader(name = "Authorization", required = false) String token) {
        AuthenticatedUser user = authenticate(token);
        authorizationService.requireRole(user, UserRole.WORKER);
        return Map.of("tasks", 3, "message", "Worker dashboard data");
    }

    private AuthenticatedUser authenticate(String token) {
        try {
            return authService.authenticate(token);
        } catch (SecurityException se) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, se.getMessage(), se);
        }
    }
}
