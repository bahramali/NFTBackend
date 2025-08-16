package se.hydroleaf.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "latest_sensor_value",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_lsv_device_sensor", columnNames = {"composite_id", "sensor_type"})
        },
        indexes = {
                @Index(name = "ix_lsv_sensor_device", columnList = "sensor_type, composite_id")
        }
)
@Data
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

    @Column(name = "sensor_type", nullable = false)
    private String sensorType;

    @Column(name = "value")
    private Double value;

    @Column(name = "unit")
    private String unit;

    @Column(name = "value_time", nullable = false)
    private Instant timestamp;
}
