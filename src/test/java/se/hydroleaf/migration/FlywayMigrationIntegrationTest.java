package se.hydroleaf.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import se.hydroleaf.bootstrap.SuperAdminSeeder;
import se.hydroleaf.store.config.StoreDataSeeder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("flyway-it")
@Testcontainers
class FlywayMigrationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("nft")
                    .withUsername("postgres")
                    .withPassword("postgres")
                    // creates minimal legacy schema BEFORE Flyway starts
                    .withInitScript("db/flyway-it/init_legacy_schema.sql");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);

        r.add("spring.flyway.enabled", () -> "true");

        // Fix: schema is non-empty (legacy), so baseline flyway history first
        r.add("spring.flyway.baseline-on-migrate", () -> "true");
        r.add("spring.flyway.baseline-version", () -> "0");

        // Prevent Hibernate from trying to validate/create schema in this IT
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");

        // Disable unrelated services
        r.add("mqtt.enabled", () -> "false");
        r.add("mqtt.publishEnabled", () -> "false");
    }

    /**
     * IMPORTANT: Prevent app startup runners from touching the DB.
     * SuperAdminSeeder queries app_user.role which doesn't exist in our minimal legacy schema.
     */
    @MockBean
    private SuperAdminSeeder superAdminSeeder;

    @MockBean
    private StoreDataSeeder storeDataSeeder;

    @Autowired
    private DataSource dataSource;

    @Test
    void migrationBackfillsNotificationFlagsForExistingUsers() throws Exception {
        // Existing legacy row (id=1) should be backfilled to TRUE/TRUE
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "select order_confirmation_emails, pickup_ready_notification from app_user where id = ?"
             )) {
            ps.setLong(1, 1L);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getBoolean("order_confirmation_emails")).isTrue();
                assertThat(rs.getBoolean("pickup_ready_notification")).isTrue();
            }
        }

        // New row inserted without the two columns should get DEFAULT TRUE/TRUE
        try (Connection c = dataSource.getConnection();
             Statement st = c.createStatement()) {
            st.executeUpdate("insert into app_user (id) values (2)");
        }

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "select order_confirmation_emails, pickup_ready_notification from app_user where id = ?"
             )) {
            ps.setLong(1, 2L);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getBoolean("order_confirmation_emails")).isTrue();
                assertThat(rs.getBoolean("pickup_ready_notification")).isTrue();
            }
        }

        // NOT NULL enforcement: explicitly inserting NULL should fail
        assertThatThrownBy(() -> {
            try (Connection c = dataSource.getConnection();
                 Statement st = c.createStatement()) {
                st.executeUpdate(
                        "insert into app_user (id, order_confirmation_emails, pickup_ready_notification) " +
                                "values (3, null, null)"
                );
            }
        }).hasMessageContaining("null value in column");
    }
}
