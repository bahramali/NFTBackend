package se.hydroleaf.websocket;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Tracks active WebSocket sessions for monitoring purposes.
 */
@Component
public class WebSocketSessionTracker {

    private final AtomicInteger sessions = new AtomicInteger();

    public int increment() {
        return sessions.incrementAndGet();
    }

    public int decrement() {
        return sessions.decrementAndGet();
    }

    public int getSessionCount() {
        return sessions.get();
    }
}
