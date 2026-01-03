package se.hydroleaf.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;
import se.hydroleaf.controller.dto.AcceptInviteRequest;
import se.hydroleaf.controller.dto.AccessTokenResponse;
import se.hydroleaf.controller.dto.CustomerRegistrationRequest;
import se.hydroleaf.controller.dto.InviteValidationResponse;
import se.hydroleaf.controller.dto.LoginRequest;
import se.hydroleaf.controller.dto.LoginResponse;
import se.hydroleaf.controller.dto.PasswordResetConfirmRequest;
import se.hydroleaf.controller.dto.PasswordResetResponse;
import se.hydroleaf.controller.dto.RefreshTokenRequest;
import se.hydroleaf.model.User;
import se.hydroleaf.service.AuthService;
import se.hydroleaf.service.AuthenticatedUser;
import se.hydroleaf.service.AdminLifecycleService;
import se.hydroleaf.service.PasswordResetService;
import se.hydroleaf.service.RefreshTokenCookieService;
import se.hydroleaf.service.UserService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AdminLifecycleService adminLifecycleService;
    private final UserService userService;
    private final PasswordResetService passwordResetService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        try {
            AuthService.LoginResult result = authService.login(
                    request.email(),
                    request.password(),
                    servletRequest.getHeader("User-Agent"),
                    servletRequest.getRemoteAddr()
            );
            AuthenticatedUser user = result.user();
            List<String> permissions = user.permissions().stream().map(Enum::name).toList();
            LoginResponse response = new LoginResponse(user.userId(), user.role(), permissions, result.accessToken());
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookieService.createRefreshCookie(result.refreshToken()).toString())
                    .body(response);
        } catch (SecurityException se) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, se.getMessage(), se);
        }
    }

    @PostMapping("/accept-invite")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<LoginResponse> acceptInvite(
            @Valid @RequestBody AcceptInviteRequest request,
            HttpServletRequest servletRequest
    ) {
        User activated = adminLifecycleService.acceptInvite(request.token(), request.password());
        AuthService.LoginResult result = authService.login(
                activated.getEmail(),
                request.password(),
                servletRequest.getHeader("User-Agent"),
                servletRequest.getRemoteAddr()
        );
        AuthenticatedUser user = result.user();
        List<String> permissions = user.permissions().stream().map(Enum::name).toList();
        LoginResponse response = new LoginResponse(user.userId(), user.role(), permissions, result.accessToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieService.createRefreshCookie(result.refreshToken()).toString())
                .body(response);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<LoginResponse> register(
            @Valid @RequestBody CustomerRegistrationRequest request,
            HttpServletRequest servletRequest
    ) {
        User created = userService.registerCustomer(request);
        AuthService.LoginResult result = authService.login(
                created.getEmail(),
                request.password(),
                servletRequest.getHeader("User-Agent"),
                servletRequest.getRemoteAddr()
        );
        AuthenticatedUser user = result.user();
        List<String> permissions = user.permissions().stream().map(Enum::name).toList();
        LoginResponse response = new LoginResponse(user.userId(), user.role(), permissions, result.accessToken());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieService.createRefreshCookie(result.refreshToken()).toString())
                .body(response);
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<AccessTokenResponse> refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest servletRequest
    ) {
        try {
            String refreshToken = resolveRefreshToken(servletRequest, request);
            AuthService.RefreshResult result = authService.refreshAccessToken(
                    refreshToken,
                    servletRequest.getHeader("User-Agent"),
                    servletRequest.getRemoteAddr()
            );
            AccessTokenResponse response = new AccessTokenResponse(result.accessToken());
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookieService.createRefreshCookie(result.refreshToken()).toString())
                    .body(response);
        } catch (SecurityException se) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, se.getMessage(), se);
        }
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest servletRequest
    ) {
        try {
            String refreshToken = resolveRefreshToken(servletRequest, request);
            authService.logout(refreshToken);
            return ResponseEntity.noContent()
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookieService.clearRefreshCookie().toString())
                    .build();
        } catch (SecurityException se) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, se.getMessage(), se);
        }
    }

    @GetMapping("/accept-invite/{token}")
    @ResponseStatus(HttpStatus.OK)
    public InviteValidationResponse validateInvite(@PathVariable("token") String token) {
        AdminLifecycleService.InviteValidationResult validation = adminLifecycleService.validateInvite(token);
        return new InviteValidationResponse(validation.email(), validation.displayName(), validation.expiresAt());
    }

    @PostMapping(value = "/password-reset", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public PasswordResetResponse passwordReset(@RequestHeader(name = "Authorization", required = false) String token) {
        passwordResetService.requestPasswordReset(token);
        return PasswordResetResponse.success();
    }

    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.OK)
    public PasswordResetResponse confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirmPasswordReset(request.token(), request.password());
        return PasswordResetResponse.success();
    }

    private String resolveRefreshToken(HttpServletRequest servletRequest, RefreshTokenRequest request) {
        if (servletRequest.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : servletRequest.getCookies()) {
                if (refreshTokenCookieService.cookieName().equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return request != null ? request.refreshToken() : null;
    }
}
