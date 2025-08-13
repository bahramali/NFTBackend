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
            WHERE sr.device_composite_id = :compositeId
                  AND sr.record_time BETWEEN :from AND :to
            GROUP BY sd.sensor_name, sd.value_type, sd.unit, bucketTime
            ORDER BY sd.sensor_name, sd.value_type, sd.unit, bucketTime
            """, nativeQuery = true)
    List<SensorAggregateResult> aggregateSensorData(
            @Param("compositeId") String compositeId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("bucketSize") long bucketSizeInSeconds
    );

    @Query(value = """
            WITH last_rec AS (
              SELECT
                sr.id,
                sr.device_composite_id,
                ROW_NUMBER() OVER (
                  PARTITION BY sr.device_composite_id
                  ORDER BY sr.record_time DESC
                ) AS rn
              FROM sensor_record sr
              JOIN device d ON d.composite_id = sr.device_composite_id
              WHERE d.system = :system AND d.layer = :layer
            )
            SELECT
              COALESCE(AVG(sd.sensor_value), 0)                     AS average,
              CAST(COUNT(sd.sensor_value) AS INTEGER)               AS count
            FROM last_rec lr
            JOIN sensor_data sd ON sd.record_id = lr.id
            WHERE lr.rn = 1                                         -- latest per device
              AND sd.sensor_name = :sensorName
            """, nativeQuery = true)
    AverageResult getLatestAverage(
            @Param("system") String system,
            @Param("layer") String layer,
            @Param("sensorName") String sensorName
    );

}
