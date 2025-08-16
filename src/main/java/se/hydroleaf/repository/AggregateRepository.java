package se.hydroleaf.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class AggregateRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AggregateRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private record Config(String from, String deviceCol, String typeCol, String valueExpr, String timeCol) {}

    private static final Map<String, Config> CONFIGS = Map.of(
            "actuator_status", new Config(
                    "actuator_status a",
                    "a.composite_id",
                    "a.actuator_type",
                    "CASE WHEN a.state THEN 1.0 ELSE 0.0 END",
                    "a.status_time"
            ),
            "sensor_data", new Config(
                    "sensor_record sr JOIN sensor_data sd ON sd.record_id = sr.id",
                    "sr.device_composite_id",
                    "sd.sensor_type",
                    "sd.sensor_value",
                    "sr.record_time"
            )
    );

    public AverageCount getLatestAverage(String system, String layer, String type, String tableName) {
        Config cfg = CONFIGS.get(tableName);
        if (cfg == null) {
            throw new IllegalArgumentException("Unknown table: " + tableName);
        }

        String sql = String.format("""
            WITH latest AS (
              SELECT
                %1$s AS composite_id,
                %2$s AS val,
                %5$s AS ts,
                ROW_NUMBER() OVER (
                  PARTITION BY %1$s
                  ORDER BY %5$s DESC
                ) AS rn
              FROM %4$s
              JOIN device d ON d.composite_id = %1$s
              WHERE d.system = :system AND d.layer = :layer AND %3$s = :type
            )
            SELECT
              COALESCE(AVG(val), 0) AS average,
              CAST(COUNT(val) AS BIGINT) AS count
            FROM latest
            WHERE rn = 1
            """, cfg.deviceCol, cfg.valueExpr, cfg.typeCol, cfg.from, cfg.timeCol);

        Map<String, Object> params = Map.of(
                "system", system,
                "layer", layer,
                "type", type
        );

        return jdbcTemplate.queryForObject(sql, params, (rs, rowNum) ->
                new AverageCount(rs.getDouble("average"), rs.getLong("count"))
        );
    }
}

