package se.hydroleaf.model;

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
public class SensorHealthItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sensorType;  // e.g. "sht3x", "veml7700"
    private Boolean status;

    @ManyToOne
    @JoinColumn(name = "record_id")
    private SensorRecord record;
}
