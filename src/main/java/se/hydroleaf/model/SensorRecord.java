package se.hydroleaf.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "sensor_record")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class SensorRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant timestamp;

    // Each record belongs to one device
    @ManyToOne
    @JoinColumn(name = "device_id")
    private Device device;

    // One record contains many sensor data items
    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL)
    private List<SensorData> sensors;

    // One SensorRecord contains many health
    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL)
    private List<SensorHealthItem> health;
}
