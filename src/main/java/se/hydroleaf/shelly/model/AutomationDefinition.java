package se.hydroleaf.shelly.model;

import java.time.LocalTime;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AutomationDefinition {
    String automationId;
    AutomationType type;
    String socketId;

    // TIME_RANGE
    LocalTime onTime;
    LocalTime offTime;
    Set<String> daysOfWeek;

    // INTERVAL_TOGGLE
    Integer intervalMinutes;
    IntervalMode mode;
    Integer pulseSeconds;

    // AUTO_OFF
    Integer durationMinutes;
    Boolean startNow;
}
