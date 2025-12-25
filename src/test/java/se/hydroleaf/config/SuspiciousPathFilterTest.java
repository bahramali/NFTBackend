package se.hydroleaf.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SuspiciousPathFilterTest {

    private final SuspiciousPathFilter filter = new SuspiciousPathFilter();

    @ParameterizedTest
    @ValueSource(strings = {
            "/wp-login.php",
            "/wp-admin/",
            "/something.php",
            "/xmlrpc.php",
            "/.env",
            "/vendor/composer/installed.json",
            "/cgi-bin/test.cgi"
    })
    void blocksSuspiciousPaths(String path) {
        assertThat(filter.isSuspiciousPath(path)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/orders",
            "/store/checkout",
            "/assets/app.js",
            "/favicon.ico"
    })
    void allowsSafePaths(String path) {
        assertThat(filter.isSuspiciousPath(path)).isFalse();
    }
}
