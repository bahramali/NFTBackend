package se.hydroleaf.shelly.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.Set;
import lombok.Data;
import se.hydroleaf.shelly.model.AutomationType;
import se.hydroleaf.shelly.model.IntervalMode;

@Data
public class AutomationRequest {
    @NotNull
    private AutomationType type;

    @NotBlank
    private String socketId;

    private LocalTime onTime;
    private LocalTime offTime;
    private Set<String> daysOfWeek;

    @Min(1)
    private Integer intervalMinutes;
    private IntervalMode mode;
    @Min(1)
    private Integer pulseSeconds;

    @Min(1)
    private Integer durationMinutes;
    private Boolean startNow;
}
