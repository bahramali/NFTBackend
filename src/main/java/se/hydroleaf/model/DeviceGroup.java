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
public class DeviceGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String mqttTopic;

    // One device group contains many devices
    @OneToMany(mappedBy = "group")
    private List<Device> devices;
}
