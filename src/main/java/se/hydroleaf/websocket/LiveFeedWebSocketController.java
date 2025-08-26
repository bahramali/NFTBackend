package se.hydroleaf.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import se.hydroleaf.repository.dto.snapshot.LiveNowSnapshot;
import se.hydroleaf.service.StatusService;

/**
 * Handles WebSocket requests for live feed data.
 */
@Slf4j
@Controller
public class LiveFeedWebSocketController {

    private final StatusService statusService;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionTracker sessionTracker;

    public LiveFeedWebSocketController(StatusService statusService,
                                       ObjectMapper objectMapper,
                                       SimpMessagingTemplate messagingTemplate,
                                       WebSocketSessionTracker sessionTracker) {
        this.statusService = statusService;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
        this.sessionTracker = sessionTracker;
    }

    @MessageMapping("/live-now")
    public void liveNow() {
        try {
            LiveNowSnapshot snapshot = statusService.getLiveNowSnapshot();
            String payload = objectMapper.writeValueAsString(snapshot);
            messagingTemplate.convertAndSend("/topic/live_now", payload);
        } catch (DataAccessException dae) {
            log.warn("DB access failed for live feed request (sessions: {})", sessionTracker.getSessionCount(), dae);
            messagingTemplate.convertAndSend("/topic/live_now", "{\"error\":\"data unavailable\"}");
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize LiveNowSnapshot", e);
        } catch (Exception e) {
            log.warn("liveNow message handling failed", e);
        }
    }
}
