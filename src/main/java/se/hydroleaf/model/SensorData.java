package se.hydroleaf.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
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

    @Column(columnDefinition = "TEXT")
    private String value; // can be number, string, or JSON structure

    // Each sensor data belongs to one sensor record
    @ManyToOne
    @JoinColumn(name = "record_id")
    private SensorRecord record;
}
