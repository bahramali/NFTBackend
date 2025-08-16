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
@Getter
@Setter
@EqualsAndHashCode(exclude = "group")
@ToString(exclude = "group")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "device",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ux_device_system_layer_deviceid",
                        columnNames = {"system", "layer", "device_id"}
                )
        },
        indexes = {
                @Index(name = "ix_device_system_layer", columnList = "system,layer"),
                @Index(name = "ix_device_device_id", columnList = "device_id")
        }
)
public class Device {

    @Id
    @Column(name = "composite_id", length = 128, nullable = false)
    private String compositeId;                 // e.g. S01-L02-esp32-01

    @Column(name = "system", length = 16, nullable = false)
    private String system;                      // e.g. S01

    @Column(name = "layer", length = 16, nullable = false)
    private String layer;                       // e.g. L02

    @Column(name = "device_id", length = 64, nullable = false)
    private String deviceId;                    // e.g. esp32-01   <-- (NEW)

    @ManyToOne(optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private DeviceGroup group;
}
