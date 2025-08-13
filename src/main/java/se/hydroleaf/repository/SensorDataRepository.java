package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import se.hydroleaf.model.SensorData;

import java.time.Instant;
import java.util.List;

@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, Long> {

    // Aggregates sensor data using the explicit column names `sensor_name` and `value_type`
    // to match the fields in AggregatedSensorData and SensorData entities.
    @Query(value = """
                SELECT
                  sd.sensor_name AS sensorName,
                  sd.value_type AS valueType,
                  sd.unit AS unit,
                  to_timestamp(floor(extract(epoch FROM sr.record_time) / :bucketSize) * :bucketSize) AS bucketTime,
                  AVG(sd.sensor_value) AS avgValue
                FROM sensor_data sd
                JOIN sensor_record sr ON sd.record_id = sr.id
            WHERE sr.device_id = :deviceId
                  AND sr.record_time BETWEEN :from AND :to
            GROUP BY sd.sensor_name, sd.value_type, sd.unit, bucketTime
            ORDER BY sd.sensor_name, sd.value_type, sd.unit, bucketTime
            """, nativeQuery = true)
    List<SensorAggregateResult> aggregateSensorData(
            @Param("deviceId") String deviceId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("bucketSize") long bucketSizeInSeconds
    );

    @Query(value = """
            WITH dev AS (
              SELECT id
              FROM device
              WHERE system = UPPER(:system) AND layer = UPPER(:layer)
            )
            SELECT AVG(sd.sensor_value::double precision) AS average,
                   COUNT(*) AS count
            FROM dev d
            JOIN LATERAL (
              SELECT sr.id
              FROM sensor_record sr
              WHERE sr.device_id = d.id
              ORDER BY sr.record_time DESC
              LIMIT 1
            ) lr ON true
            JOIN sensor_data sd
              ON sd.record_id = lr.id
            WHERE sd.value_type = :sensorType;
            """, nativeQuery = true)
    AverageResult getLatestAverage(
            @Param("system") String system,
            @Param("layer") String layer,
            @Param("sensorType") String sensorType
    );
}
