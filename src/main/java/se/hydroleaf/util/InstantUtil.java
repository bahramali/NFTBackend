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

    public static Instant truncateToBucket(Instant t, String bucket) {
        long sec = bucketSeconds(bucket);
        long epoch = t.getEpochSecond();
        long floored = (epoch / sec) * sec;
        return Instant.ofEpochSecond(floored);
    }

    public static long bucketSeconds(String bucket) {
        if (bucket == null) throw new IllegalArgumentException("bucket is required");
        String s = bucket.trim().toLowerCase();
        if (s.endsWith("m")) return Long.parseLong(s.substring(0, s.length() - 1)) * 60L;
        if (s.endsWith("h")) return Long.parseLong(s.substring(0, s.length() - 1)) * 3600L;
        if (s.endsWith("d")) return Long.parseLong(s.substring(0, s.length() - 1)) * 86400L;
        return Long.parseLong(s); // assume raw seconds
    }
}
