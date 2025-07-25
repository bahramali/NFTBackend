package se.hydroleaf.dto;

import java.time.Instant;

public record TimestampValue(
        Instant timestamp,
        Object value
) {}
