package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import se.hydroleaf.model.SensorValueHistory;
import se.hydroleaf.model.SensorValueHistory.SensorValueHistoryId;
import se.hydroleaf.repository.dto.SensorAggregationRow;

import java.time.Instant;
import java.util.List;

public interface SensorAggregationRepository extends Repository<SensorValueHistory, SensorValueHistoryId> {

    @Query(value = """
            SELECT
              svh.sensor_type AS sensor_type,
              lsv.unit        AS unit,
              time_bucket(:bucketSec * INTERVAL '1 second', svh.value_time) AS bucket_time,
              AVG(svh.sensor_value) AS avg_value
            FROM sensor_value_history svh
            LEFT JOIN latest_sensor_value lsv ON lsv.composite_id = svh.composite_id AND lsv.sensor_type = svh.sensor_type
            WHERE svh.composite_id = :compositeId
              AND svh.value_time >= :fromTs
              AND svh.value_time <  :toTs
              AND svh.sensor_value IS NOT NULL
              AND (:sensorType IS NULL OR svh.sensor_type = :sensorType)
            GROUP BY bucket_time, svh.sensor_type, lsv.unit
            ORDER BY bucket_time
            """, nativeQuery = true)
    List<SensorAggregationRow> aggregateTimescale(
            @Param("compositeId") String compositeId,
            @Param("fromTs") Instant from,
            @Param("toTs") Instant to,
            @Param("bucketSec") long bucketSeconds,
            @Param("sensorType") String sensorType
    );

    @Query(value = """
            SELECT
              svh.sensor_type AS sensor_type,
              lsv.unit        AS unit,
              date_trunc('second', to_timestamp(floor(EXTRACT(EPOCH FROM svh.value_time) / :bucketSec) * :bucketSec)) AS bucket_time,
              AVG(svh.sensor_value) AS avg_value
            FROM sensor_value_history svh
            LEFT JOIN latest_sensor_value lsv ON lsv.composite_id = svh.composite_id AND lsv.sensor_type = svh.sensor_type
            WHERE svh.composite_id = :compositeId
              AND svh.value_time >= :fromTs
              AND svh.value_time <  :toTs
              AND svh.sensor_value IS NOT NULL
              AND (:sensorType IS NULL OR svh.sensor_type = :sensorType)
            GROUP BY bucket_time, svh.sensor_type, lsv.unit
            ORDER BY bucket_time
            """, nativeQuery = true)
    List<SensorAggregationRow> aggregateDateTrunc(
            @Param("compositeId") String compositeId,
            @Param("fromTs") Instant from,
            @Param("toTs") Instant to,
            @Param("bucketSec") long bucketSeconds,
            @Param("sensorType") String sensorType
    );
}
