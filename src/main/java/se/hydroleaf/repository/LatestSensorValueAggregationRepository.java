package se.hydroleaf.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import se.hydroleaf.model.LatestSensorValue;
import se.hydroleaf.repository.dto.LiveNowRow;

import java.util.Collection;
import java.util.List;

public interface LatestSensorValueAggregationRepository extends Repository<LatestSensorValue, Long> {

    @Query(value = """
            SELECT
              d.system AS system,
              d.layer AS layer,
              lsv.sensor_type AS sensor_type,
              MAX(lsv.unit) AS unit,
              AVG(lsv.sensor_value)::double precision AS avg_value,
              COUNT(*)::bigint AS device_count,
              MAX(lsv.value_time) AS record_time
            FROM latest_sensor_value lsv
            JOIN device d ON d.composite_id = lsv.composite_id
            WHERE lsv.sensor_type IN (:types)
            GROUP BY d.system, d.layer, lsv.sensor_type
            """, nativeQuery = true)
    List<LiveNowRow> fetchLatestSensorAverages(@Param("types") Collection<String> types);
}
