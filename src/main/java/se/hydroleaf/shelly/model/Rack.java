package se.hydroleaf.shelly.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Rack {
    String id;
    String name;
    String roomId;
    String telemetryRackId;
}
