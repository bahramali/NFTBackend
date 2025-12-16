package se.hydroleaf.shelly.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SocketStatusDTO {
    String socketId;
    boolean output;
    Double powerW;
    Double voltageV;
    boolean online;
    Instant lastUpdated;
}
