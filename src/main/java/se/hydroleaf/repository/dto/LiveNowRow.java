package se.hydroleaf.repository.dto;

import java.time.Instant;

/**
 * Projection representing the latest average reading for a sensor or actuator.
 */
public record LiveNowRow(
        String system,
        String layer,
        String sensorType,
        Instant lastUpdate,
        Double avgValue
) {
    /**
     * Returns the system identifier.
     */
    public String getSystem() {
        return system;
    }

    /**
     * Returns the layer name.
     */
    public String getLayer() {
        return layer;
    }

    /**
     * Returns the sensor or actuator type.
     */
    public String getSensorType() {
        return sensorType;
    }

    /**
     * Returns the timestamp for the latest value.
     */
    public Instant getLastUpdate() {
        return lastUpdate;
    }

    /**
     * Returns the averaged value for the given type.
     */
    public Double getAvgValue() {
        return avgValue;
    }
}
