package se.hydroleaf.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

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
    private Double numericValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "spectrum_json", columnDefinition = "jsonb")
    private Map<String, Integer> spectrumJson;

    @Column(name = "value", columnDefinition = "text")  // can be number, string, or JSON structure
    private String sensorValue;
    // Each sensor data belongs to one sensor record
    @ManyToOne
    @JoinColumn(name = "record_id")
    private SensorRecord record;
}
