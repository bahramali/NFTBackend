package se.hydroleaf.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "sensor_value_history",
       indexes = {
           @Index(name = "idx_svh_device_sensor_time", columnList = "composite_id, sensor_type, value_time DESC"),
           @Index(name = "idx_svh_device_time", columnList = "composite_id, value_time"),
           @Index(name = "idx_svh_system_layer", columnList = "system_part, layer_part")
       })
@IdClass(SensorValueHistoryId.class)
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorValueHistory {

    @Id
    @Column(name = "value_time", nullable = false)
    @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
    private Instant valueTime;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @Column(name = "system_part", length = 64)
    private String systemPart;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @Column(name = "layer_part", length = 64)
    private String layerPart;

    @Id
    @Column(name = "composite_id", nullable = false, length = 128)
    private String compositeId;

    @Id
    @Column(name = "sensor_type", nullable = false, length = 64)
    private String sensorType;

    @Column(name = "sensor_value")
    private Double sensorValue;

    @PrePersist
    @PreUpdate
    private void fillParts() {
        if (compositeId != null) {
            String[] parts = compositeId.split("-", 4);
            systemPart = parts.length > 0 ? parts[0] : null;
            layerPart = parts.length > 2 ? parts[2] : null;
        } else {
            systemPart = null;
            layerPart = null;
        }
    }
}
