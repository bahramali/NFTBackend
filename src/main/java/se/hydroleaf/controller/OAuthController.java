package se.hydroleaf.controller;

import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import se.hydroleaf.controller.dto.LoginResponse;
import se.hydroleaf.controller.dto.OAuthProviderResponse;
import se.hydroleaf.controller.dto.OAuthStartRequest;
import se.hydroleaf.controller.dto.OAuthStartResponse;
import se.hydroleaf.model.OauthProvider;
import se.hydroleaf.service.OAuthLoginService;
import se.hydroleaf.service.AuthenticatedUser;
import se.hydroleaf.service.RefreshTokenCookieService;

@RestController
@RequestMapping("/api/auth/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthLoginService oauthLoginService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    @GetMapping("/providers")
    public List<OAuthProviderResponse> listProviders() {
        return List.of(new OAuthProviderResponse("google", "Google"));
    }

    @PostMapping("/google/start")
    public OAuthStartResponse startGoogle(
            @RequestBody(required = false) OAuthStartRequest request
    ) {
        String redirectUri = request != null ? request.redirectUri() : null;
        String callbackBaseUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .build()
                .toUriString();
        OAuthLoginService.OAuthStartResult result = oauthLoginService.startLogin(
                OauthProvider.GOOGLE,
                redirectUri,
                callbackBaseUrl
        );
        return new OAuthStartResponse(result.authorizationUrl());
    }

    @GetMapping("/google/callback")
    public ResponseEntity<?> handleGoogleCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state
    ) {
        OAuthLoginService.OAuthLoginResult result = oauthLoginService.handleCallback(OauthProvider.GOOGLE, code, state);
        AuthenticatedUser user = result.loginResult().user();
        List<String> permissions = user.permissions().stream()
                .map(Enum::name)
                .toList();
        LoginResponse response = new LoginResponse(
                user.userId(),
                user.role(),
                permissions,
                result.loginResult().accessToken()
        );
        String refreshCookie = refreshTokenCookieService.createRefreshCookie(result.loginResult().refreshToken()).toString();

        if (result.redirectUri() == null || result.redirectUri().isBlank()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie)
                    .body(response);
        }

        URI redirect = UriComponentsBuilder.fromUriString(result.redirectUri())
                .queryParam("success", 1)
                .queryParam("token", result.loginResult().accessToken())
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(redirect);
        headers.add(HttpHeaders.SET_COOKIE, refreshCookie);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
