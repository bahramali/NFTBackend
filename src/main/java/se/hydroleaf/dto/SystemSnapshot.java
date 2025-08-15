package se.hydroleaf.dto;

import java.time.Instant;

public record SystemSnapshot(
        Instant lastUpdate,
        LayerActuatorStatus actuators,
        WaterTankSummary water,
        GrowSensorSummary environment
) {}
