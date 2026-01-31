package se.hydroleaf.controller.dto;

public record MonitoringPageResponse(
        Long id,
        String title,
        String slug,
        String rackId,
        String telemetryRackId,
        int sortOrder
) {
}
