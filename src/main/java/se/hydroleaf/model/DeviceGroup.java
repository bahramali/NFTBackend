package se.hydroleaf.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "device_group",
        indexes = {
                @Index(name = "ux_device_group_mqtt_topic", columnList = "mqtt_topic", unique = true)
        }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class DeviceGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Optional: a unique topic or logical name for the group.
     */
    @Column(name = "mqtt_topic", unique = true)
    private String mqttTopic;

    /**
     * One group can contain many devices.
     */
    @OneToMany(mappedBy = "group", orphanRemoval = false)
    private List<Device> devices = new ArrayList<>();
}
