package se.hydroleaf.repository;

import java.time.Instant;

public interface SensorAggregateResult {
    String getSensorName();
    String getValueType();
    String getUnit();
    Instant getBucketTime();
    Double getAvgValue();
}
