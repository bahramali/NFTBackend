package se.hydroleaf.repository.dto.snapshot;

import java.time.Instant;
import java.util.List;

import se.hydroleaf.repository.dto.summary.ActuatorStatusSummary;
import se.hydroleaf.repository.dto.summary.GrowSensorSummary;
import se.hydroleaf.repository.dto.summary.WaterTankSummary;

public record SystemSnapshot(
        Instant lastUpdate,
        ActuatorStatusSummary actuators,
        WaterTankSummary water,
        GrowSensorSummary environment,
        List<LayerSnapshot> layers
) {

    public record LayerSnapshot(
            String layerId,
            Instant lastUpdate,
            ActuatorStatusSummary actuators,
            WaterTankSummary water,
            GrowSensorSummary environment
    ) {}
}

