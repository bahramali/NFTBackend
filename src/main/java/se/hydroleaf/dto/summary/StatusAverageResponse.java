package se.hydroleaf.dto.summary;

/**
 * Represents an average value for a sensor or actuator together with its unit
 * and how many devices contributed to the average.
 */
public record StatusAverageResponse(Double average, String unit, long deviceCount) {}

