package se.hydroleaf.repository.dto;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * Projection for native queries returning a mix of numeric and temporal columns.
 * The numeric and time-based fields are defined with broad types so that
 * Hibernate can instantiate the record regardless of the exact JDBC types
 * produced by different dialects. Accessors convert the values to the expected
 * Java types used by the service layer.
 */
public record LiveNowRow(
        String system,
        String layer,
        String sensorType,
        String unit,
        Number avgValue,
        Number deviceCount,
        Object recordTime
) {
    public String getSystem() { return system; }
    public String getLayer() { return layer; }
    public String getSensorType() { return sensorType; }
    public String getUnit() { return unit; }

    public Double getAvgValue() { return avgValue != null ? avgValue.doubleValue() : null; }

    public Long getDeviceCount() { return deviceCount != null ? deviceCount.longValue() : null; }

    public Instant getRecordTime() {
        if (recordTime == null) {
            return null;
        }
        if (recordTime instanceof Instant instant) {
            return instant;
        }
        if (recordTime instanceof Timestamp ts) {
            return ts.toInstant();
        }
        if (recordTime instanceof OffsetDateTime odt) {
            return odt.toInstant();
        }
        if (recordTime instanceof java.util.Date date) {
            return date.toInstant();
        }
        throw new IllegalArgumentException("Unsupported recordTime type: " + recordTime.getClass());
    }
}
