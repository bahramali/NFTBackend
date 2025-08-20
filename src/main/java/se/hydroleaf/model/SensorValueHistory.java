package se.hydroleaf.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
    @Column(name = "composite_id", nullable = false, length = 128)
    private String compositeId;

    @Id
    @Column(name = "sensor_type", nullable = false, length = 64)
    private String sensorType;

    @Id
    @Column(name = "value_time", nullable = false)
    private Instant valueTime;

    @Column(name = "sensor_value")
    private Double sensorValue;

    @Column(name = "system_part", length = 64, insertable = false, updatable = false)
    private String systemPart;

    @Column(name = "layer_part", length = 64, insertable = false, updatable = false)
    private String layerPart;
}
