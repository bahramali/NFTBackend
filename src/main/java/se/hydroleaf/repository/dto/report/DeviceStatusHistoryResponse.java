package se.hydroleaf.repository.dto.report;

import java.time.Instant;

public record DeviceStatusHistoryResponse(
        String compositeId,
        String status,
        Instant statusTime
) {
}
