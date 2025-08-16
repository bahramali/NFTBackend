package se.hydroleaf.scheduler;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Registry keeping track of when devices were last seen.
 */
public class LastSeenRegistry {

    private final ConcurrentHashMap<String, Instant> lastSeen = new ConcurrentHashMap<>();

    /**
     * Updates the registry with the current time for the given composite id.
     *
     * @param compositeId device composite id
     */
    public void update(String compositeId) {
        lastSeen.put(compositeId, Instant.now());
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
}

