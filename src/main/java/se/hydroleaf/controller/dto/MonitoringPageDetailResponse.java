package se.hydroleaf.controller.dto;

public record MonitoringPageDetailResponse(
        Long id,
        String title,
        String slug,
        String rackId,
        int sortOrder,
        boolean enabled
) {
}
