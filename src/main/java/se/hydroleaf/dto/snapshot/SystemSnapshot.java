package se.hydroleaf.dto.snapshot;

import java.time.Instant;
import java.util.List;

import se.hydroleaf.dto.summary.ActuatorStatusSummary;
import se.hydroleaf.dto.summary.GrowSensorSummary;
import se.hydroleaf.dto.summary.WaterTankSummary;

/**
 * Snapshot of a system containing all layer snapshots.
 */
public record SystemSnapshot(
        Instant lastUpdate,
        ActuatorStatusSummary actuators,
        WaterTankSummary water,
        GrowSensorSummary environment,
        List<LayerSnapshot> layers
) {

    /**
     * Snapshot of a single layer with the time of the last update.
     * Holds water, environment and actuator information for the layer.
     */
    public record LayerSnapshot(
            String layerId,
            Instant lastUpdate,
            ActuatorStatusSummary actuators,
            WaterTankSummary water,
            GrowSensorSummary environment
    ) {}
}

