package se.hydroleaf.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

/**
 * JPA entity mapping the latest_sensor_value table. This table stores the most
 * recent sensor reading per device and sensor type and is normally maintained by
 * a database trigger. The entity exists primarily to allow tests to populate the
 * table when running with an in-memory database.
 */
@Entity
@Table(
        name = "latest_sensor_value",
        indexes = {
                @Index(name = "idx_lsv_device_sensor", columnList = "composite_id, sensor_type")
        }
)
@Getter
@Setter
@EqualsAndHashCode(exclude = "device")
@ToString(exclude = "device")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LatestSensorValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "composite_id", referencedColumnName = "composite_id", nullable = false)
    private Device device;

    @Column(name = "sensor_type", nullable = false, length = 64)
    private String sensorType;

    @Column(name = "sensor_value")
    private Double value;

    @Column(name = "unit", length = 32)
    private String unit;

    @Column(name = "value_time", nullable = false)
    private Instant valueTime;
}

