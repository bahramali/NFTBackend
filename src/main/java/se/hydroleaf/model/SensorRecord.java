package se.hydroleaf.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "sensor_record",
        indexes = {
                @Index(name = "ix_record_device_time", columnList = "device_composite_id, record_time DESC")
        }
)
@Getter
@Setter
@EqualsAndHashCode(exclude = {"device", "readings", "healthItems"})
@ToString(exclude = {"device", "readings", "healthItems"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Device foreign key via composite_id.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "device_composite_id", referencedColumnName = "composite_id", nullable = false)
    private Device device;

    /**
     * When this record was captured. Set default at DB, but keep it non-null here too.
     */
    @Column(name = "record_time", nullable = false)
    private Instant timestamp;

    /**
     * Record contains many readings (measurements).
     */
    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SensorReading> readings = new ArrayList<>();

    /**
     * Record can also contain health items.
     */
    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SensorHealthItem> healthItems = new ArrayList<>();
}
