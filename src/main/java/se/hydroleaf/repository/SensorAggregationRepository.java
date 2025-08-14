package se.hydroleaf.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import se.hydroleaf.model.SensorRecord;

import java.time.Instant;
import java.util.List;

@Repository
public interface SensorAggregationRepository extends JpaRepository<SensorRecord, Long> {

    interface Row {
        String getSensorType();

        String getUnit();

        Instant getBucketTime();

        Double getAvgValue();
    }

    @Query(value = """
            SELECT
              to_timestamp(floor(extract(epoch from sr.record_time) / :bucketSec) * :bucketSec) AS bucket_time,
            sd.sensor_type AS sensor_type,
            sd.unit        AS unit,
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
    List<Row> aggregate(
            @Param("compositeId") String compositeId,
            @Param("fromTs") Instant from,
            @Param("toTs") Instant to,
            @Param("bucketSec") long bucketSeconds
    );
}