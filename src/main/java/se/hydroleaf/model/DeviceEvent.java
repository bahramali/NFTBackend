package se.hydroleaf.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
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
@Table(name = "device_event",
        indexes = {
                @Index(name = "ix_device_event_device_time", columnList = "composite_id, event_time DESC")
        })
@Getter
@Setter
@EqualsAndHashCode(exclude = "device")
@ToString(exclude = "device")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "level", length = 16)
    private String level;

    @Column(name = "code", length = 64)
    private String code;

    @Lob
    @Column(name = "msg")
    private String msg;

    @Column(name = "raw", columnDefinition = "jsonb")
    private String raw;

    @ManyToOne(optional = false)
    @JoinColumn(name = "composite_id", referencedColumnName = "composite_id", nullable = false)
    private Device device;
}
