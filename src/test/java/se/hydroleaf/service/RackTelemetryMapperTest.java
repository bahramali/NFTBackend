package se.hydroleaf.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RackTelemetryMapperTest {

    private final RackTelemetryMapper rackTelemetryMapper = new RackTelemetryMapper();

    @Test
    void resolveTelemetryRackIdUsesProvidedTelemetryRackId() {
        String resolved = rackTelemetryMapper.resolveTelemetryRackId("RACK_01", "R99");

        assertThat(resolved).isEqualTo("R99");
    }

    @Test
    void resolveTelemetryRackIdFallsBackToRackIdWhenNoTelemetryRackId() {
        String resolved = rackTelemetryMapper.resolveTelemetryRackId("rack-alpha", null);

        assertThat(resolved).isEqualTo("rack-alpha");
    }

    @Test
    void resolveTelemetryRackIdReturnsNullWhenInputsMissing() {
        String resolved = rackTelemetryMapper.resolveTelemetryRackId(null, null);

        assertThat(resolved).isNull();
    }
}
