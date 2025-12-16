package se.hydroleaf.shelly.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ErrorResponse {
    String socketId;
    String message;
    SocketStatusDTO status;
}
