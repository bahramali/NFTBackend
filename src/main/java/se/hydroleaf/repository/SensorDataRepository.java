package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.hydroleaf.model.SensorData;

import java.time.Instant;
import java.util.List;

public interface SensorDataRepository extends JpaRepository<SensorData, Long> {

    interface BucketAggregation {
        String getSensorId();
        String getType();
        String getUnit();
        Instant getBucketTime();
        Double getAvgValue();
    }

    @Query(value = """
            SELECT sd.sensor_id AS sensorId,
                   sd.type AS type,
                   sd.unit AS unit,
                   to_timestamp(floor(extract(epoch FROM sr.record_time) / :interval) * :interval) AS bucketTime,
                   AVG(CAST(sd.value AS double precision)) AS avgValue
            FROM sensor_data sd
            JOIN sensor_record sr ON sd.record_id = sr.id
            WHERE sr.device_id = :deviceId
              AND sr.record_time BETWEEN :from AND :to
            GROUP BY sd.sensor_id, sd.type, sd.unit, to_timestamp(floor(extract(epoch FROM sr.record_time) / :interval) * :interval)
            ORDER BY sd.sensor_id, sd.type, sd.unit, bucketTime
            """,
            nativeQuery = true)
    List<BucketAggregation> aggregateByDeviceAndInterval(
            @Param("deviceId") String deviceId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("interval") long intervalSeconds
    );
}
