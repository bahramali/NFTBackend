package se.hydroleaf.repository;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Detects whether the TimescaleDB extension is available. Detection can be
 * forced by setting the configuration property {@code app.timescaledb.enabled}.
 * If the property is not provided, a query against {@code pg_extension} is
 * executed on startup.
 */
@Component
public class TimescaleDbSupport implements InitializingBean {

    private final JdbcTemplate jdbcTemplate;
    private final Boolean configured;
    private boolean available;

    public TimescaleDbSupport(JdbcTemplate jdbcTemplate,
                              @Value("${app.timescaledb.enabled:#{null}}") Boolean configured) {
        this.jdbcTemplate = jdbcTemplate;
        this.configured = configured;
    }

    @Override
    public void afterPropertiesSet() {
        if (configured != null) {
            available = configured;
            return;
        }
        try {
            Integer res = jdbcTemplate.queryForObject(
                    "SELECT 1 FROM pg_extension WHERE extname = 'timescaledb'",
                    Integer.class);
            available = res != null;
        } catch (Exception ex) {
            available = false;
        }
    }

    public boolean isAvailable() {
        return available;
    }
}
