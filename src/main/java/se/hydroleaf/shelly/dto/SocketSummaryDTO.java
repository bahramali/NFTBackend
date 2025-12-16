package se.hydroleaf.shelly.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SocketSummaryDTO {
    String id;
    String name;
    String rackId;
}
