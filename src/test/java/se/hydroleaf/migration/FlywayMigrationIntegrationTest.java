package se.hydroleaf.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("flyway-it")
@Testcontainers
class FlywayMigrationIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.locations",
                () -> "classpath:db/test-migration,classpath:db/migration");
    }

    @Autowired
    private DataSource dataSource;

    @Test
    void migrationBackfillsNotificationFlagsForExistingUsers() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "select order_confirmation_emails, pickup_ready_notification from app_user where id = 1"
             )) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getBoolean("order_confirmation_emails")).isTrue();
            assertThat(resultSet.getBoolean("pickup_ready_notification")).isTrue();
        }

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet orderDefaults = statement.executeQuery(
                     "select is_nullable, column_default from information_schema.columns "
                             + "where table_name = 'app_user' and column_name = 'order_confirmation_emails'"
             )) {
            assertThat(orderDefaults.next()).isTrue();
            assertThat(orderDefaults.getString("is_nullable")).isEqualTo("NO");
            assertThat(orderDefaults.getString("column_default")).contains("true");
        }

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet pickupDefaults = statement.executeQuery(
                     "select is_nullable, column_default from information_schema.columns "
                             + "where table_name = 'app_user' and column_name = 'pickup_ready_notification'"
             )) {
            assertThat(pickupDefaults.next()).isTrue();
            assertThat(pickupDefaults.getString("is_nullable")).isEqualTo("NO");
            assertThat(pickupDefaults.getString("column_default")).contains("true");
        }
    }
}
