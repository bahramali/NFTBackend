package se.hydroleaf.repository.dto;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;

public record LiveNowRow(
        String system,
        String layer,
        String sensorType,
        String unit,
        Number avgValue,
        Number deviceCount,
        Object recordTime
) {
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
