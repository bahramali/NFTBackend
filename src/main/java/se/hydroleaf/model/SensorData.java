package se.hydroleaf.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "sensor_data")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class SensorData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sensor_name")
    private String sensorName;

    @Column(name = "value_type")
    private String valueType;

    private String unit;

    @Column(name = "value")
    private Double value;

    @Column(name = "source")
    private String source;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "record_id")
    private SensorRecord record;
}
