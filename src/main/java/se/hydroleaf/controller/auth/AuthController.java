package se.hydroleaf.controller.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.model.User;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.security.JwtProvider;
import se.hydroleaf.security.RefreshTokenService;
import se.hydroleaf.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager,
                          JwtProvider jwtProvider,
                          RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    @PreAuthorize("permitAll()")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserRole role = request.role() != null ? request.role() : UserRole.USER;
        User user = userService.registerUser(request.email(), request.password(), role);
        UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
        String accessToken = jwtProvider.generateAccessToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(buildAuthResponse(accessToken, refreshToken));
    }

    @PostMapping("/login")
    @PreAuthorize("permitAll()")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                request.email().toLowerCase(),
                request.password()
        );
        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String accessToken = jwtProvider.generateAccessToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername());
        return ResponseEntity.ok(buildAuthResponse(accessToken, refreshToken));
    }

    @PostMapping("/refresh")
    @PreAuthorize("permitAll()")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        String email = refreshTokenService.validateAndGetEmail(request.refreshToken());
        refreshTokenService.revokeToken(request.refreshToken());
        UserDetails userDetails = userService.loadUserByUsername(email);
        String accessToken = jwtProvider.generateAccessToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(email);
        return ResponseEntity.ok(buildAuthResponse(accessToken, refreshToken));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String principalEmail = authentication != null ? authentication.getName() : null;
        if (request.refreshToken() != null && !request.refreshToken().isBlank()) {
            String tokenEmail = refreshTokenService.validateAndGetEmail(request.refreshToken());
            if (principalEmail != null && !principalEmail.equalsIgnoreCase(tokenEmail)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Refresh token does not belong to the authenticated user");
            }
            refreshTokenService.revokeToken(request.refreshToken());
        } else if (principalEmail != null) {
            refreshTokenService.revokeTokensForUser(principalEmail);
        }
        return ResponseEntity.noContent().build();
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken) {
        long accessExpiresIn = Math.max(jwtProvider.getAccessTokenValidity().toSeconds(), 0);
        long refreshExpiresIn = Math.max(jwtProvider.getRefreshTokenValidity().toSeconds(), 0);
        return new AuthResponse(accessToken, accessExpiresIn, refreshToken, refreshExpiresIn);
    }

    public record RegisterRequest(@Email @NotBlank String email,
                                  @NotBlank @Size(min = 8, max = 128) String password,
                                  UserRole role) {
    }

    public record LoginRequest(@Email @NotBlank String email,
                               @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(String refreshToken) {
    }

    public record AuthResponse(String accessToken,
                               long accessTokenExpiresIn,
                               String refreshToken,
                               long refreshTokenExpiresIn) {
    }
}
