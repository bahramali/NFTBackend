package se.hydroleaf.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorValueHistoryId implements Serializable {
    private String compositeId;
    private String sensorType;
    private Instant valueTime;
}
