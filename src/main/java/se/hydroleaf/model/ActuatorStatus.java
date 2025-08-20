package se.hydroleaf.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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

    @Column(name = "status_time", nullable = false)
    private Instant timestamp;

    @Column(name = "actuator_type", nullable = false)
    private String actuatorType;

    @Column(name = "state", nullable = false)
    private Boolean state;

    @ManyToOne(optional = false)
    @JoinColumn(name = "composite_id", referencedColumnName = "composite_id", nullable = false)
    private Device device;
}
