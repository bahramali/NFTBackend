package se.hydroleaf.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InitialSchemaTest {
    @Test
    void initialSchemaContainsTimescaleExtension() throws IOException {
        String schema = Files.readString(Path.of("src/main/resources/db/migration/initial_schema.sql"));
        assertTrue(schema.contains("CREATE EXTENSION IF NOT EXISTS timescaledb;"),
                "initial_schema.sql must include TimescaleDB extension");
    }
}
