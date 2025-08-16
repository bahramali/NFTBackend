package se.hydroleaf.scheduler;

import org.junit.jupiter.api.Test;

import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

class LastSeenRegistryTest {

    static class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        void add(Duration duration) {
            instant = instant.plus(duration);
        }
    }

    @Test
    void evictsEntriesBeyondMaxAge() {
        MutableClock clock = new MutableClock(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
        LastSeenRegistry registry = new LastSeenRegistry(Duration.ofSeconds(60), 0, clock);

        registry.update("old");
        clock.add(Duration.ofSeconds(30));
        registry.update("fresh");
        clock.add(Duration.ofSeconds(31));

        registry.cleanup();

        assertFalse(registry.contains("old"));
        assertTrue(registry.contains("fresh"));
    }

    @Test
    void enforcesMaxSize() {
        MutableClock clock = new MutableClock(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
        LastSeenRegistry registry = new LastSeenRegistry(null, 2, clock);

        registry.update("d1");
        clock.add(Duration.ofSeconds(1));
        registry.update("d2");
        clock.add(Duration.ofSeconds(1));
        registry.update("d3");

        assertFalse(registry.contains("d1"));
        assertTrue(registry.contains("d2"));
        assertTrue(registry.contains("d3"));
    }
}
