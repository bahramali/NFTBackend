package se.hydroleaf.repository;

/**
 * Projection representing a distinct system and layer combination for devices.
 */
public record SystemLayer(String system, String layer) {

    public String getSystem() {
        return system;
    }

    public String getLayer() {
        return layer;
    }
}

