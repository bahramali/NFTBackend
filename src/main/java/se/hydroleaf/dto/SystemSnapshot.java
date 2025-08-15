package se.hydroleaf.dto;

import java.time.Instant;
import java.util.List;

/**
 * Snapshot of a system containing all layer snapshots.
 */
public record SystemSnapshot(
        List<LayerSnapshot> layers
) {

    /**
     * Snapshot of a single layer with the time of the last update.
     * Holds water, environment and actuator information for the layer.
     */
    public record LayerSnapshot(
            String layerId,
            Instant lastUpdate,
            LayerActuatorStatus actuators,
            WaterTankSummary water,
            GrowSensorSummary environment
    ) {}
}

