package se.hydroleaf.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import se.hydroleaf.model.TopicName;

/**
 * Ensures the {@code device_topic_check} constraint is aligned with the
 * {@link TopicName} enum. The constraint predates the introduction of the
 * {@code germinationTopic} MQTT topic and would prevent devices that publish on
 * the new topic from being registered.
 */
@Component
public class DeviceTopicConstraintUpdater implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DeviceTopicConstraintUpdater.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public DeviceTopicConstraintUpdater(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!isPostgres()) {
            log.debug("Skipping device topic constraint update for non-PostgreSQL database");
            return;
        }

        String allowedTopics = Arrays.stream(TopicName.values())
                .map(Enum::name)
                .map(name -> "'" + name + "'")
                .collect(Collectors.joining(", "));

        jdbcTemplate.execute("ALTER TABLE device DROP CONSTRAINT IF EXISTS device_topic_check");
        jdbcTemplate.execute("ALTER TABLE device ADD CONSTRAINT device_topic_check CHECK (topic IN (" + allowedTopics + "))");

        log.info("Updated device_topic_check constraint to allow topics: {}", allowedTopics);
    }

    private boolean isPostgres() {
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            return "PostgreSQL".equalsIgnoreCase(productName);
        } catch (SQLException e) {
            log.warn("Failed to determine database product, assuming non-PostgreSQL", e);
            return false;
        }
    }
}

