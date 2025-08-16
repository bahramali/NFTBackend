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

/**
 * Generic actuator status persisted for each controller item.
 */
@Entity
@Table(name = "actuator_status",
        indexes = {
                @Index(name = "ix_as_device_time", columnList = "composite_id, status_time DESC")
        })
@Getter
@Setter
@EqualsAndHashCode(exclude = "device")
@ToString(exclude = "device")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActuatorStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Time when this status was recorded.
     */
    @Column(name = "status_time", nullable = false)
    private Instant timestamp;

    /**
     * Type of actuator, e.g. airPump, light etc.
     */
    @Column(name = "actuator_type", nullable = false)
    private String actuatorType;

    /**
     * Actuator state (on/off).
     */
    @Column(name = "state", nullable = false)
    private Boolean state;

    /**
     * Owning device via composite_id.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "composite_id", referencedColumnName = "composite_id", nullable = false)
    private Device device;
}
