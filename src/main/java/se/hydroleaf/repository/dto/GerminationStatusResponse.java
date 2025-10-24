package se.hydroleaf.repository.dto;

import java.time.Instant;

public record GerminationStatusResponse(
        String compositeId,
        Instant startTime,
        long elapsedSeconds
) {
}

