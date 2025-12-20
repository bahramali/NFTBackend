package se.hydroleaf.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@EqualsAndHashCode
@ToString
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
    private String compositeId;

    @Column(name = "system", length = 16, nullable = false)
    private String system;

    @Column(name = "layer", length = 16, nullable = false)
    private String layer;

    @Column(name = "device_id", length = 64, nullable = false)
    private String deviceId;

    @Column(name = "name", length = 128)
    private String name;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "topic", length = 64, nullable = false)
    private TopicName topic;
}
