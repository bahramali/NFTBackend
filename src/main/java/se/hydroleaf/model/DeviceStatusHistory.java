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
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "device_status_history",
        indexes = {
                @Index(name = "ix_device_status_history_device_time", columnList = "composite_id, status_time DESC")
        })
@Getter
@Setter
@EqualsAndHashCode(exclude = "device")
@ToString(exclude = "device")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "status_time", nullable = false)
    private Instant timestamp;

    @ManyToOne(optional = false)
    @JoinColumn(name = "composite_id", referencedColumnName = "composite_id", nullable = false)
    private Device device;
}
