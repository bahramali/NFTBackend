package se.hydroleaf.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Listens for WebSocket connect and disconnect events to maintain session counts.
 */
@Slf4j
@Component
public class WebSocketSessionListener {

    private final WebSocketSessionTracker tracker;

    public WebSocketSessionListener(WebSocketSessionTracker tracker) {
        this.tracker = tracker;
    }

    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        int count = tracker.increment();
        log.info("WebSocket session connected. Active sessions: {}", count);
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        int count = tracker.decrement();
        log.info("WebSocket session disconnected. Active sessions: {}", count);
    }
}
