package se.hydroleaf.dto;

import java.time.Instant;
import java.util.List;

/**
 * Snapshot of a system containing categorized layer snapshots for water and environment.
 */
public record SystemSnapshot(
        CategorySnapshot water,
        CategorySnapshot environment
) {

    /**
     * Wrapper for layer snapshots categorized by type.
     */
    public record CategorySnapshot(
            List<LayerSnapshot> byLayer
    ) {}

    /**
     * Snapshot of a single layer with the time of the last update.
     */
    public record LayerSnapshot(
            String layerId,
            Instant lastUpdate,
            LayerActuatorStatus actuators,
            WaterTankSummary water,
            GrowSensorSummary environment
    ) {}
}

