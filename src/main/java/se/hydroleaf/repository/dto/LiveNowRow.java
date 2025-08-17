package se.hydroleaf.repository.dto;

import java.time.Instant;

public record LiveNowRow(
        String system,
        String layer,
        String sensorType,
        String unit,
        Double avgValue,
        Long deviceCount,
        Instant recordTime
) {
    public String getSystem() { return system; }
    public String getLayer() { return layer; }
    public String getSensorType() { return sensorType; }
    public String getUnit() { return unit; }
    public Double getAvgValue() { return avgValue; }
    public Long getDeviceCount() { return deviceCount; }
    public Instant getRecordTime() { return recordTime; }
}

