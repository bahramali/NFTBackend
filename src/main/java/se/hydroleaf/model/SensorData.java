package se.hydroleaf.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "sensor_data",
        uniqueConstraints = {
                // Prevent duplicate (record, sensor_type) rows.
                @UniqueConstraint(name = "ux_data_record_type", columnNames = {"record_id", "sensor_type"})
        },
        indexes = {
                @Index(name = "ix_data_record", columnList = "record_id")
        }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class SensorData {

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
     * Logical sensor type: e.g. "light", "temperature", "humidity", "ph", "ec", "do", "air_pump".
     */
    @Column(name = "sensor_type", nullable = false, length = 64)
    private String sensorType;

    /**
     * Numeric value for most sensors. Column name avoids the reserved word "value".
     */
    @Column(name = "sensor_value")
    private Double value;

    /**
     * Optional unit (e.g., "lx","°C","%","mg/L","mS/cm").
     */
    @Column(name = "unit", length = 32)
    private String unit;

    /**
     * Optional source tag (e.g., "avg", "raw", "composite").
     */
    @Column(name = "source", length = 64)
    private String source;
}
