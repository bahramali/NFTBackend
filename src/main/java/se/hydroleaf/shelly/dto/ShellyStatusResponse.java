package se.hydroleaf.shelly.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ShellyStatusResponse {
    private String deviceId;
    private boolean output;
}
