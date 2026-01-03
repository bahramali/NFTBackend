package se.hydroleaf.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import se.hydroleaf.config.AuthProperties;

@Service
@RequiredArgsConstructor
public class RefreshTokenCookieService {

    private final AuthProperties authProperties;

    public ResponseCookie createRefreshCookie(String refreshToken) {
        return ResponseCookie.from(authProperties.getCookie().getName(), refreshToken)
                .httpOnly(authProperties.getCookie().isHttpOnly())
                .secure(authProperties.getCookie().isSecure())
                .path(authProperties.getCookie().getPath())
                .sameSite(authProperties.getCookie().getSameSite())
                .maxAge(authProperties.getRefresh().getTokenTtl())
                .build();
    }

    public ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(authProperties.getCookie().getName(), "")
                .httpOnly(authProperties.getCookie().isHttpOnly())
                .secure(authProperties.getCookie().isSecure())
                .path(authProperties.getCookie().getPath())
                .sameSite(authProperties.getCookie().getSameSite())
                .maxAge(0)
                .build();
    }

    public String cookieName() {
        return authProperties.getCookie().getName();
    }
}
