package se.hydroleaf.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.controller.dto.AcceptInviteRequest;
import se.hydroleaf.controller.dto.CustomerRegistrationRequest;
import se.hydroleaf.controller.dto.InviteValidationResponse;
import se.hydroleaf.controller.dto.LoginRequest;
import se.hydroleaf.controller.dto.LoginResponse;
import se.hydroleaf.controller.dto.PasswordResetResponse;
import se.hydroleaf.model.User;
import se.hydroleaf.service.AuthService;
import se.hydroleaf.service.AuthenticatedUser;
import se.hydroleaf.service.AdminLifecycleService;
import se.hydroleaf.service.PasswordResetService;
import se.hydroleaf.service.UserService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AdminLifecycleService adminLifecycleService;
    private final UserService userService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthService.LoginResult result = authService.login(request.email(), request.password());
            AuthenticatedUser user = result.user();
            List<String> permissions = user.permissions().stream().map(Enum::name).toList();
            return new LoginResponse(user.userId(), user.role(), permissions, result.token());
        } catch (SecurityException se) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, se.getMessage(), se);
        }
    }

    @PostMapping("/accept-invite")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse acceptInvite(@Valid @RequestBody AcceptInviteRequest request) {
        User activated = adminLifecycleService.acceptInvite(request.token(), request.password());
        AuthService.LoginResult result = authService.login(activated.getEmail(), request.password());
        AuthenticatedUser user = result.user();
        List<String> permissions = user.permissions().stream().map(Enum::name).toList();
        return new LoginResponse(user.userId(), user.role(), permissions, result.token());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse register(@Valid @RequestBody CustomerRegistrationRequest request) {
        User created = userService.registerCustomer(request);
        AuthService.LoginResult result = authService.login(created.getEmail(), request.password());
        AuthenticatedUser user = result.user();
        List<String> permissions = user.permissions().stream().map(Enum::name).toList();
        return new LoginResponse(user.userId(), user.role(), permissions, result.token());
    }

    @GetMapping("/accept-invite/{token}")
    @ResponseStatus(HttpStatus.OK)
    public InviteValidationResponse validateInvite(@PathVariable("token") String token) {
        AdminLifecycleService.InviteValidationResult validation = adminLifecycleService.validateInvite(token);
        return new InviteValidationResponse(validation.email(), validation.displayName(), validation.expiresAt());
    }

    @PostMapping("/password-reset")
    @ResponseStatus(HttpStatus.OK)
    public PasswordResetResponse passwordReset(@RequestHeader(name = "Authorization", required = false) String token) {
        passwordResetService.requestPasswordReset(token);
        return PasswordResetResponse.success();
    }
}
