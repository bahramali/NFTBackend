package se.hydroleaf.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.controller.dto.LoginRequest;
import se.hydroleaf.controller.dto.LoginResponse;
import se.hydroleaf.service.AuthService;
import se.hydroleaf.service.AuthenticatedUser;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthService.LoginResult result = authService.login(request.credential(), request.password());
            AuthenticatedUser user = result.user();
            List<String> permissions = user.permissions().stream().map(Enum::name).toList();
            return new LoginResponse(user.userId(), user.role(), permissions, result.token());
        } catch (SecurityException se) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, se.getMessage(), se);
        }
    }
}
