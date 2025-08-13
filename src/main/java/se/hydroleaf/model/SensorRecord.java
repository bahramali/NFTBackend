package se.hydroleaf.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
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

    @Column(name = "record_time")
    private Instant timestamp;

    // Each record belongs to one device
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "device_composite_id", referencedColumnName = "composite_id")
    private Device device;

    // One record contains many sensor data items
    @ToString.Exclude
    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL)
    private List<SensorData> sensors;

    // One SensorRecord contains many health
    @ToString.Exclude
    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL)
    private List<SensorHealthItem> health;
}
