package se.hydroleaf.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
        name = "device_group",
        indexes = {
                @Index(name = "ux_device_group_mqtt_topic", columnList = "mqtt_topic", unique = true)
        }
)
@Getter
@Setter
@EqualsAndHashCode
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Optional: a unique topic or logical name for the group.
     */
    @Column(name = "mqtt_topic", unique = true)
    private String mqttTopic;

}
