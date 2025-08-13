package se.hydroleaf.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "oxygen_pump_status",
        indexes = {
                @Index(name = "ix_ops_device_time", columnList = "composite_id, status_time DESC")
        }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class OxygenPumpStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Time when this status was recorded. Keep NOT NULL at DB level as well.
     */
    @Column(name = "status_time", nullable = false)
    private Instant timestamp;

    /**
     * Pump on/off.
     */
    @Column(name = "status", nullable = false)
    private Boolean status;

    /**
     * Foreign key to Device via composite_id (no parallel free-text fields).
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "composite_id", referencedColumnName = "composite_id", nullable = false)
    private Device device;
}
