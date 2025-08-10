package se.hydroleaf.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "oxygen_pump_status")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class OxygenPumpStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "status_time")
    private Instant timestamp;

    private Boolean status;

    private String system;

    private String layer;
}
