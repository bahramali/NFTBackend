package se.hydroleaf.scheduler;

import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Registry keeping track of when devices were last seen.
 *
 * <p>The registry can be configured with a maximum age and size. Entries exceeding
 * these limits are removed either on update or during the periodic cleanup task.</p>
 */
public class LastSeenRegistry {

    private final ConcurrentHashMap<String, Instant> lastSeen = new ConcurrentHashMap<>();
    private final Duration maxAge;
    private final int maxSize;
    private final Clock clock;

    public LastSeenRegistry() {
        this(null, 0, Clock.systemUTC());
    }

    public LastSeenRegistry(Duration maxAge, int maxSize) {
        this(maxAge, maxSize, Clock.systemUTC());
    }

    public LastSeenRegistry(Duration maxAge, int maxSize, Clock clock) {
        this.maxAge = maxAge;
        this.maxSize = maxSize;
        this.clock = clock;
    }

    /**
     * Updates the registry with the current time for the given composite id.
     *
     * @param compositeId device composite id
     */
    public void update(String compositeId) {
        lastSeen.put(compositeId, Instant.now(clock));
        enforceLimits();
    }

    /**
     * Iterates over all registry entries.
     *
     * @param action action to perform for each entry
     */
    public void forEach(BiConsumer<? super String, ? super Instant> action) {
        lastSeen.forEach(action);
    }

    /**
     * Whether the registry contains the given id.
     *
     * @param compositeId device composite id
     * @return true if the id exists in the registry
     */
    public boolean contains(String compositeId) {
        return lastSeen.containsKey(compositeId);
    }

    /**
     * Whether the registry is empty.
     *
     * @return true if no entries exist
     */
    public boolean isEmpty() {
        return lastSeen.isEmpty();
    }

    /**
     * Removes entries exceeding max age or size.
     */
    @Scheduled(fixedDelayString = "${lastseen.cleanup-interval:60000}")
    public void cleanup() {
        enforceLimits();
    }

    private void enforceLimits() {
        if (maxAge != null && !maxAge.isZero() && !lastSeen.isEmpty()) {
            Instant cutoff = Instant.now(clock).minus(maxAge);
            lastSeen.entrySet().removeIf(e -> e.getValue().isBefore(cutoff));
        }
        if (maxSize > 0 && lastSeen.size() > maxSize) {
            while (lastSeen.size() > maxSize) {
                String oldestKey = null;
                Instant oldestInstant = null;
                for (Entry<String, Instant> e : lastSeen.entrySet()) {
                    if (oldestInstant == null || e.getValue().isBefore(oldestInstant)) {
                        oldestInstant = e.getValue();
                        oldestKey = e.getKey();
                    }
                }
                if (oldestKey == null) {
                    break;
                }
                lastSeen.remove(oldestKey, oldestInstant);
            }
        }
    }
}

