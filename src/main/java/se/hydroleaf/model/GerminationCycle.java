package se.hydroleaf.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "germination_cycle")
@Getter
@Setter
@NoArgsConstructor
public class GerminationCycle implements Persistable<String> {

    @Id
    @Column(name = "composite_id", length = 128, nullable = false)
    private String compositeId;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "composite_id")
    private Device device;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Transient
    private boolean isNew = true;

    public GerminationCycle(Device device, Instant startTime) {
        this.device = device;
        this.compositeId = device.getCompositeId();
        this.startTime = startTime;
    }

    @Override
    public String getId() {
        return compositeId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }
}

