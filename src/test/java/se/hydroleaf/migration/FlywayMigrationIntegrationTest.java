package se.hydroleaf.migration;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration,classpath:db/test-migration"
})
@ActiveProfiles("test")
class FlywayMigrationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationBackfillsNotificationFlagsForExistingUsers() {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "select order_confirmation_emails, pickup_ready_notification from app_user where id = 1"
        );

        assertThat(row.get("order_confirmation_emails")).isEqualTo(Boolean.TRUE);
        assertThat(row.get("pickup_ready_notification")).isEqualTo(Boolean.TRUE);
    }
}
