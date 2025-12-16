package se.hydroleaf.shelly.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SocketDevice {
    String id;
    String name;
    String rackId;
    String ip;
    int relayIndex;
    String username;
    String password;
}
