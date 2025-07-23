package se.hydroleaf.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Device {
    @Id
    private String id; // e.g., esp32-01

    private String location;

    // Each device belongs to one device group
    @ManyToOne
    @JoinColumn(name = "group_id")
    private DeviceGroup group;

    // One device can have many sensor records
    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL)
    private List<SensorRecord> sensorRecords;
}
