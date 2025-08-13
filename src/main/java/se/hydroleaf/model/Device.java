package se.hydroleaf.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(
        name = "device",
        indexes = {
                // Helpful when filtering by system/layer; keep them if you store these columns.
                @Index(name = "ix_device_system", columnList = "system"),
                @Index(name = "ix_device_layer", columnList = "layer")
        }
)
public class Device {
    /**
     * Primary identifier, e.g. "S01-L01-esp32-01". This is the single source of truth.
     */
    @Id
    @Column(name = "composite_id", nullable = false, updatable = false)
    private String compositeId;

    /**
     * Optional denormalized fields. Keep only if you enforce consistency with compositeId.
     */
    @Column(name = "system")
    private String system;

    @Column(name = "layer")
    private String layer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private DeviceGroup group;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Device d)) return false;
        return compositeId != null && compositeId.equals(d.compositeId);
    }

    @Override
    public int hashCode() {
        return compositeId != null ? compositeId.hashCode() : 0;
    }

}
