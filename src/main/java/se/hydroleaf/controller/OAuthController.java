package se.hydroleaf.controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
import se.hydroleaf.config.OAuthProperties;
import se.hydroleaf.model.OauthProvider;
import se.hydroleaf.service.OAuthLoginService;
import se.hydroleaf.service.AuthenticatedUser;
import se.hydroleaf.service.RefreshTokenCookieService;

@RestController
@RequestMapping("/api/auth/oauth")
@RequiredArgsConstructor
@Slf4j
public class OAuthController {

    private final OAuthLoginService oauthLoginService;
    private final RefreshTokenCookieService refreshTokenCookieService;
    private final OAuthProperties oauthProperties;

    @GetMapping("/providers")
    public List<OAuthProviderResponse> listProviders() {
        List<OAuthProviderResponse> providers = new ArrayList<>();
        if (StringUtils.hasText(oauthProperties.getGoogle().getClientId())) {
            providers.add(new OAuthProviderResponse("google", "Google"));
        }
        return providers;
    }

    @PostMapping("/google/start")
    public OAuthStartResponse startGoogle(
            @RequestBody(required = false) OAuthStartRequest request
    ) {
        String redirectUri = request != null ? request.redirectUri() : null;
        String callbackBaseUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .build()
                .toUriString();
        log.info("Google OAuth start requested. redirectHost={}, callbackHost={}",
                hostOnly(redirectUri),
                hostOnly(callbackBaseUrl));
        OAuthLoginService.OAuthStartResult result = oauthLoginService.startLogin(
                OauthProvider.GOOGLE,
                redirectUri,
                callbackBaseUrl
        );
        log.info("Google OAuth start completed. statePrefix={}, redirectHost={}",
                statePrefix(result.state()),
                hostOnly(redirectUri));
        return new OAuthStartResponse(result.authorizationUrl());
    }

    @GetMapping("/google/callback")
    public ResponseEntity<?> handleGoogleCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state
    ) {
        if (!StringUtils.hasText(code) || !StringUtils.hasText(state)) {
            return oauthError(
                    HttpStatus.BAD_REQUEST,
                    "missing_params",
                    "code and state are required"
            );
        }
        log.info("Google OAuth callback received. statePrefix={}", statePrefix(state));
        OAuthLoginService.OAuthLoginResult result;
        try {
            result = oauthLoginService.handleCallback(OauthProvider.GOOGLE, code, state);
        } catch (org.springframework.web.server.ResponseStatusException ex) {
            HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
            if (status == HttpStatus.UNAUTHORIZED && "Invalid or expired OAuth state".equals(ex.getReason())) {
                return oauthError(status, "invalid_state", "invalid_state");
            }
            if (status == HttpStatus.BAD_GATEWAY || status == HttpStatus.GATEWAY_TIMEOUT) {
                return oauthError(status, "token_exchange_failed", "token_exchange_failed");
            }
            String message = ex.getReason() != null ? ex.getReason() : "oauth_error";
            return oauthError(status, "oauth_error", message);
        }
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
            log.info("Google OAuth callback completed. statePrefix={}, redirectHost=none", statePrefix(state));
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie)
                    .body(response);
        }

        log.info("Google OAuth callback completed. statePrefix={}, redirectHost={}",
                statePrefix(state),
                hostOnly(result.redirectUri()));

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

    private ResponseEntity<OAuthErrorResponse> oauthError(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(new OAuthErrorResponse(error, message));
    }

    private record OAuthErrorResponse(String error, String message) {
    }

    private String hostOnly(String uri) {
        if (!StringUtils.hasText(uri)) {
            return "none";
        }
        URI parsed = URI.create(uri);
        if (!StringUtils.hasText(parsed.getHost())) {
            return "none";
        }
        if (parsed.getPort() > 0) {
            return parsed.getHost() + ":" + parsed.getPort();
        }
        return parsed.getHost();
    }

    private String statePrefix(String state) {
        if (!StringUtils.hasText(state)) {
            return "none";
        }
        int prefixLength = Math.min(state.length(), 8);
        return state.substring(0, prefixLength);
    }
}
