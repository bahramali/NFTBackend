package se.hydroleaf.repository.dto.history;

import java.time.Instant;

public record TimestampValue(
        Instant timestamp,
        Object value
) {}
