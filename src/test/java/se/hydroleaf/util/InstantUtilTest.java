package se.hydroleaf.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class InstantUtilTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "2024-01-02T03:04:05Z",
            "2024-01-02T03:04:05.123Z",
            "2024-01-02T03:04:05+02:00",
            "2024-01-02T03:04:05.123-05:00"
    })
    void parsesFlexibleFormats(String input) {
        Instant expected = OffsetDateTime.parse(input).toInstant();
        assertEquals(expected, InstantUtil.parse(input));
    }

    @Test
    void invalidInputThrows() {
        assertThrows(IllegalArgumentException.class, () -> InstantUtil.parse("invalid"));
    }
}
