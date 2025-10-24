package se.hydroleaf.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "germination_cycle")
@Getter
@Setter
@NoArgsConstructor
public class GerminationCycle {

    @Id
    @Column(name = "composite_id", length = 128, nullable = false)
    private String compositeId;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "composite_id")
    private Device device;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    public GerminationCycle(Device device, Instant startTime) {
        this.device = device;
        this.compositeId = device.getCompositeId();
        this.startTime = startTime;
    }
}

