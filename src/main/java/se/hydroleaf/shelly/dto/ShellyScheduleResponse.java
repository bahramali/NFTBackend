package se.hydroleaf.shelly.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ShellyScheduleResponse {
    String deviceId;
    boolean scheduled;
    LocalDateTime turnOnAt;
    LocalDateTime turnOffAt;
}
