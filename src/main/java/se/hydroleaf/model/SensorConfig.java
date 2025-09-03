package se.hydroleaf.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * JPA entity representing ideal configuration for a sensor type.
 */
@Entity
@Table(
        name = "sensor_config",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ux_sensor_config_sensor_type",
                        columnNames = {"sensor_type"}
                )
        }
)
@Getter
@Setter
@EqualsAndHashCode
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sensor_type", length = 64, nullable = false)
    private String sensorType;

    @NotNull
    @Column(name = "min_value", nullable = false)
    private Double minValue;

    @NotNull
    @Column(name = "max_value", nullable = false)
    private Double maxValue;

    @Column(name = "description", length = 512)
    private String description;
}

