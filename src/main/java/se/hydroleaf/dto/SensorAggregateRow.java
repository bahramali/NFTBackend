package se.hydroleaf.dto;

import java.time.Instant;

public interface SensorAggregateRow {
    String getSensorName();
    String getValueType();
    String getUnit();
    Instant getBucketTime();
    Double getAvgValue();
}