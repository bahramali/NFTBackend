package se.hydroleaf.shelly.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShellySwitchStatus {
    private int id;
    private boolean output;
    private String source;
}
