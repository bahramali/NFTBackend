package se.hydroleaf.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class BotProbeFilterTest {

    private final BotProbeFilter filter = new BotProbeFilter();

    @ParameterizedTest
    @ValueSource(strings = {
            "/wp-login.php",
            "/wp-admin/",
            "/something.php",
            "/.env",
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
