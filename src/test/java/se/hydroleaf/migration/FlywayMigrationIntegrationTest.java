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
        "spring.flyway.init-sqls=CREATE TABLE app_user ("
                + "id bigint primary key, "
                + "email varchar(128) not null, "
                + "password varchar(255) not null, "
                + "role varchar(32) not null, "
                + "active boolean not null, "
                + "status varchar(32) not null, "
                + "invited boolean not null, "
                + "created_at timestamp not null"
                + ");"
                + "INSERT INTO app_user (id, email, password, role, active, status, invited, created_at) "
                + "VALUES (1, 'test@example.com', 'secret', 'ADMIN', true, 'ACTIVE', false, CURRENT_TIMESTAMP);"
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
