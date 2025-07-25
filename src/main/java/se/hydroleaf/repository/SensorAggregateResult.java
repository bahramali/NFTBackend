package se.hydroleaf.repository;

import java.time.Instant;

public interface SensorAggregateResult {
    String getSensorId();
    String getType();
    String getUnit();
    Instant getBucketTime();
    Double getAvgValue();
}
