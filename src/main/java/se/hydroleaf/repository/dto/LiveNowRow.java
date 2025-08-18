package se.hydroleaf.repository.dto;

import java.sql.Timestamp;
import java.time.Instant;

public record LiveNowRow(
        String system,
        String layer,
        String sensorType,
        String unit,
        Number avgValue,
        Number deviceCount,
        Timestamp recordTime
) {
    public String getSystem() { return system; }
    public String getLayer() { return layer; }
    public String getSensorType() { return sensorType; }
    public String getUnit() { return unit; }
    public Double getAvgValue() {
        return avgValue != null ? avgValue.doubleValue() : null;
    }

    public Long getDeviceCount() {
        return deviceCount != null ? deviceCount.longValue() : null;
    }

    public Instant getRecordTime() {
        return recordTime != null ? recordTime.toInstant() : null;
    }
}
