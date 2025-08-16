package se.hydroleaf.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.hydroleaf.model.SensorRecord;
import se.hydroleaf.repository.dto.SensorAggregationRow;

import java.time.Instant;
import java.util.List;

public interface SensorAggregationRepository extends JpaRepository<SensorRecord, Long> {

    @Query(value = """
            SELECT
              sd.sensor_type AS sensor_type,
              sd.unit        AS unit,
              to_timestamp(floor(extract(epoch from sr.record_time) / :bucketSec) * :bucketSec) AS bucket_time,
              AVG(sd.sensor_value) AS avg_value
            FROM sensor_record sr
            JOIN sensor_data sd ON sd.record_id = sr.id
            WHERE sr.device_composite_id = :compositeId
              AND sr.record_time >= :fromTs
              AND sr.record_time <  :toTs
              AND sd.sensor_value IS NOT NULL
            GROUP BY bucket_time, sd.sensor_type, sd.unit
            ORDER BY bucket_time
            """, nativeQuery = true)
    List<SensorAggregationRow> aggregate(
            @Param("compositeId") String compositeId,
            @Param("fromTs") Instant from,
            @Param("toTs") Instant to,
            @Param("bucketSec") long bucketSeconds
    );
}