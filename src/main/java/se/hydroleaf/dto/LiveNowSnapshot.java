package se.hydroleaf.dto;

import java.util.Map;

public record LiveNowSnapshot(
        Map<String, Map<String, LayerSnapshot>> systems
) {
    public record LayerSnapshot(
            LayerActuatorStatus actuator,
            GrowSensorSummary growSensors,
            WaterTankSummary waterTank
    ) {}
}
