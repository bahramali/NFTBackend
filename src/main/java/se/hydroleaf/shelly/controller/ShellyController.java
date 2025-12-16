package se.hydroleaf.shelly.controller;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.shelly.dto.AutomationRequest;
import se.hydroleaf.shelly.dto.AutomationResponse;
import se.hydroleaf.shelly.dto.SocketStatusDTO;
import se.hydroleaf.shelly.dto.SocketSummaryDTO;
import se.hydroleaf.shelly.exception.ShellyException;
import se.hydroleaf.shelly.model.Rack;
import se.hydroleaf.shelly.model.Room;
import se.hydroleaf.shelly.model.SocketDevice;
import se.hydroleaf.shelly.registry.ShellyRegistry;
import se.hydroleaf.shelly.service.ShellyAutomationService;
import se.hydroleaf.shelly.service.ShellyClient;

@RestController
@RequestMapping("/api/shelly")
public class ShellyController {

    private final ShellyRegistry registry;
    private final ShellyClient clientService;
    private final ShellyAutomationService automationService;

    public ShellyController(ShellyRegistry registry, ShellyClient clientService, ShellyAutomationService automationService) {
        this.registry = registry;
        this.clientService = clientService;
        this.automationService = automationService;
    }

    @GetMapping("/rooms")
    public List<Room> getRooms() {
        return registry.getRooms().stream().toList();
    }

    @GetMapping("/rooms/{roomId}/racks")
    public List<Rack> getRacks(@PathVariable String roomId) {
        if (registry.getRoom(roomId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown room id: " + roomId);
        }
        return registry.getRacksByRoom(roomId);
    }

    @GetMapping("/racks/{rackId}/sockets")
    public List<SocketSummaryDTO> getSockets(@PathVariable String rackId) {
        if (registry.getRack(rackId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown rack id: " + rackId);
        }
        return registry.getSocketsByRack(rackId).stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @GetMapping("/sockets/{socketId}/status")
    public ResponseEntity<SocketStatusDTO> getStatus(@PathVariable String socketId) {
        SocketDevice device = resolveSocket(socketId);
        try {
            SocketStatusDTO status = clientService.getStatus(device);
            return ResponseEntity.ok(status);
        } catch (ShellyException ex) {
            SocketStatusDTO offlineStatus = offline(device);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(offlineStatus);
        }
    }

    @PostMapping("/sockets/{socketId}/on")
    public ResponseEntity<SocketStatusDTO> turnOn(@PathVariable String socketId) {
        return handleCommand(socketId, clientService::turnOn);
    }

    @PostMapping("/sockets/{socketId}/off")
    public ResponseEntity<SocketStatusDTO> turnOff(@PathVariable String socketId) {
        return handleCommand(socketId, clientService::turnOff);
    }

    @PostMapping("/sockets/{socketId}/toggle")
    public ResponseEntity<SocketStatusDTO> toggle(@PathVariable String socketId) {
        return handleCommand(socketId, clientService::toggle);
    }

    @GetMapping("/status")
    public List<SocketStatusDTO> getAllStatuses() {
        return registry.getAllSockets().stream().map(this::safeStatusLookup).toList();
    }

    @PostMapping("/automation")
    public AutomationResponse createAutomation(@Valid @RequestBody AutomationRequest request) {
        try {
            return automationService.createAutomation(request);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/automation")
    public List<AutomationResponse> listAutomations() {
        return automationService.listAutomations();
    }

    @DeleteMapping("/automation/{automationId}")
    public void deleteAutomation(@PathVariable String automationId) {
        automationService.deleteAutomation(automationId);
    }

    private ResponseEntity<SocketStatusDTO> handleCommand(String socketId, Command command) {
        SocketDevice device = resolveSocket(socketId);
        try {
            SocketStatusDTO status = command.execute(device);
            return ResponseEntity.ok(status);
        } catch (ShellyException ex) {
            SocketStatusDTO offlineStatus = offline(device);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(offlineStatus);
        }
    }

    private SocketStatusDTO safeStatusLookup(SocketDevice device) {
        try {
            return clientService.getStatus(device);
        } catch (ShellyException ex) {
            return offline(device);
        }
    }

    private SocketStatusDTO offline(SocketDevice device) {
        return SocketStatusDTO.builder()
                .socketId(device.getId())
                .output(false)
                .online(false)
                .lastUpdated(Instant.now())
                .build();
    }

    private SocketDevice resolveSocket(String socketId) {
        return registry.getSocket(socketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown socket id: " + socketId));
    }

    private SocketSummaryDTO toSummary(SocketDevice device) {
        return SocketSummaryDTO.builder()
                .id(device.getId())
                .name(device.getName())
                .rackId(device.getRackId())
                .build();
    }

    @FunctionalInterface
    private interface Command {
        SocketStatusDTO execute(SocketDevice device);
    }
}
