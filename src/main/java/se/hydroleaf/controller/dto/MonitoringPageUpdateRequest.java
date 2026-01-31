package se.hydroleaf.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MonitoringPageUpdateRequest(
        @NotBlank String title,
        String telemetryRackId,
        @NotBlank
        @Size(max = 64)
        @Pattern(regexp = "^[a-z0-9-]+$")
        String slug,
        Integer sortOrder,
        Boolean enabled
) {
}
