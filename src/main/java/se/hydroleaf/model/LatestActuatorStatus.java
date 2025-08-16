package se.hydroleaf.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "latest_actuator_status",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_las_device_actuator", columnNames = {"composite_id", "actuator_type"})
        },
        indexes = {
                @Index(name = "ix_las_actuator_device", columnList = "actuator_type, composite_id")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LatestActuatorStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "composite_id", referencedColumnName = "composite_id", nullable = false)
    private Device device;

    @Column(name = "actuator_type", nullable = false)
    private String actuatorType;

    @Column(name = "state", nullable = false)
    private Boolean state;

    @Column(name = "status_time", nullable = false)
    private Instant timestamp;
}

