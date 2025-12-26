package se.hydroleaf.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationIntegrationTest {

    @Test
    void migrationBackfillsNotificationFlagsForExistingUsers() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:flywaytest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/test-migration", "classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "select order_confirmation_emails, pickup_ready_notification from app_user where id = 1"
             )) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getBoolean("order_confirmation_emails")).isTrue();
            assertThat(resultSet.getBoolean("pickup_ready_notification")).isTrue();
        }
    }
}
