package se.hydroleaf.shelly.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShellySwitchStatus {
    private int id;
    private boolean output;
    private String source;

    @JsonProperty("apower")
    private Double activePower;

    @JsonProperty("voltage")
    private Double voltage;
}
