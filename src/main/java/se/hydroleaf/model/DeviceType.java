package se.hydroleaf.model;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Logical types for sensors and actuators. Each type is associated with a
 * default unit and information whether it represents an actuator.
 */
public enum DeviceType {
    LIGHT("light", "lux", false),
    HUMIDITY("humidity", "%", false),
    TEMPERATURE("temperature", "°C", false),
    DISSOLVED_OXYGEN("dissolvedOxygen", "mg/L", false),
    DISSOLVED_TEMP("dissolvedTemp", "°C", false),
    PH("pH", "pH", false),
    DISSOLVED_EC("dissolvedEC", "mS/cm", false),
    DISSOLVED_TDS("dissolvedTDS", "ppm", false),
    AIR_PUMP("airPump", "status", true);

    private final String name;
    private final String unit;
    private final boolean actuator;

    DeviceType(String name, String unit, boolean actuator) {
        this.name = name;
        this.unit = unit;
        this.actuator = actuator;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public boolean isActuator() {
        return actuator;
    }

    private static final Map<String, DeviceType> BY_NAME =
            Stream.of(values()).collect(Collectors.toMap(DeviceType::getName, dt -> dt));

    public static DeviceType fromName(String name) {
        if (name == null) {
            return null;
        }
        return BY_NAME.get(name);
    }
}

