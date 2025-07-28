package se.hydroleaf.util;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

public final class InstantUtil {
    private InstantUtil() {}

    private static final DateTimeFormatter FLEXIBLE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'H:mm:ss")
            .appendOffsetId()
            .toFormatter();

    public static Instant parse(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            return OffsetDateTime.parse(value, FLEXIBLE_FORMATTER).toInstant();
        }
    }
}
