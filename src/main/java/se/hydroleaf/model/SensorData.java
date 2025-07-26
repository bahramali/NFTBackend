package se.hydroleaf.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
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

    private String sensorId;
    private String type;
    private String unit;

    @Column(name = "numeric_value")
    private Double numericValue;

    @Column(name = "value", columnDefinition = "text")  // can be number, string, or JSON structure
    private String sensorValue;
    // Each sensor data belongs to one sensor record
    @ManyToOne
    @JoinColumn(name = "record_id")
    private SensorRecord record;
}
