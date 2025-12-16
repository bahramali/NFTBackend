package se.hydroleaf.shelly.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Room {
    String id;
    String name;
}
