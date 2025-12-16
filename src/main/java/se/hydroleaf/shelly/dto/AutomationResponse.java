package se.hydroleaf.shelly.dto;

import java.time.Instant;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import se.hydroleaf.shelly.model.AutomationType;
import se.hydroleaf.shelly.model.IntervalMode;

@Value
@Builder
public class AutomationResponse {
    String automationId;
    AutomationType type;
    String socketId;
    String description;
    Set<String> daysOfWeek;
    Integer intervalMinutes;
    IntervalMode mode;
    Integer pulseSeconds;
    Integer durationMinutes;
    Instant createdAt;
}
