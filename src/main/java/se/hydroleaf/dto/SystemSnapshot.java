package se.hydroleaf.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Snapshot of a system containing snapshots for each layer.
 */
public record SystemSnapshot(
        Map<String, LayerSnapshot> layers
) {

    /**
     * Snapshot of a single layer with the time of the last update.
     */
    public record LayerSnapshot(
            Instant lastUpdate,
            LayerActuatorStatus actuators,
            WaterTankSummary water,
            GrowSensorSummary environment
    ) {}
}

