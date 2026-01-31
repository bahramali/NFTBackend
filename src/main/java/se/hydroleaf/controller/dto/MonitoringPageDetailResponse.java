package se.hydroleaf.controller.dto;

public record MonitoringPageDetailResponse(
        Long id,
        String title,
        String slug,
        String rackId,
        String telemetryRackId,
        int sortOrder,
        boolean enabled
) {
}
