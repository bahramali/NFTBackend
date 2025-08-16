package se.hydroleaf.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
        name = "sensor_health_item",
        uniqueConstraints = {
                // One health item per (record, sensor_type).
                @UniqueConstraint(name = "ux_health_record_type", columnNames = {"record_id", "sensor_type"})
        },
        indexes = {
                @Index(name = "ix_health_record", columnList = "record_id")
        }
)
@Getter
@Setter
@EqualsAndHashCode(exclude = "record")
@ToString(exclude = "record")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorHealthItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent record.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "record_id", nullable = false)
    private SensorRecord record;

    /**
     * Logical sensor type, e.g., "temperature", "ph"...
     */
    @Column(name = "sensor_type", nullable = false, length = 64)
    private String sensorType;

    /**
     * Health status: true = healthy, false = out-of-range/fault.
     */
    @Column(name = "status", nullable = false)
    private Boolean status;
}
