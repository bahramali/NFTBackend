package se.hydroleaf.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.NoArgsConstructor;

import java.util.List;
@Entity
@Table(name = "device")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Device {
    @Id
    private String id;

    private String layer;

    @Column(name = "system")
    private String system;

    @Column(name = "composite_id")
    private String compositeId;

    // Each device belongs to one device group
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "group_id")
    private DeviceGroup group;

    // One device can have many sensor records
    @ToString.Exclude
    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL)
    private List<SensorRecord> sensorRecords;
}
