package se.hydroleaf.shelly.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShellyDeviceConfig {

    private String id;
    private String name;
    private String ip;
    private String username;
    private String password;
}
