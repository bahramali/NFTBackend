package se.hydroleaf.controller;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.repository.dto.report.DeviceEventResponse;
import se.hydroleaf.repository.dto.report.DeviceStatusHistoryResponse;
import se.hydroleaf.service.AuthorizationService;
import se.hydroleaf.service.DeviceStatusEventService;

@RestController
@RequestMapping("/api/devices")
public class DeviceReportController {

    private static final int DEFAULT_EVENT_LIMIT = 200;
    private static final int MAX_EVENT_LIMIT = 1000;

    private final DeviceStatusEventService deviceStatusEventService;
    private final AuthorizationService authorizationService;

    public DeviceReportController(DeviceStatusEventService deviceStatusEventService,
                                  AuthorizationService authorizationService) {
        this.deviceStatusEventService = deviceStatusEventService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/{compositeId}/status")
    public List<DeviceStatusHistoryResponse> getStatusHistory(
            @RequestHeader(name = "Authorization", required = false) String token,
            @PathVariable String compositeId,
            @RequestParam("from") String from,
            @RequestParam("to") String to) {
        authorizationService.requireMonitoringView(token);
        Instant fromInst = parseInstant(from);
        Instant toInst = parseInstant(to);
        validateRange(fromInst, toInst);

        try {
            return deviceStatusEventService.getStatusHistory(compositeId, fromInst, toInst);
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage(), iae);
        }
    }

    @GetMapping("/{compositeId}/events")
    public List<DeviceEventResponse> getEvents(
            @RequestHeader(name = "Authorization", required = false) String token,
            @PathVariable String compositeId,
            @RequestParam("from") String from,
            @RequestParam("to") String to,
            @RequestParam(name = "limit", required = false) Integer limit) {
        authorizationService.requireMonitoringView(token);
        Instant fromInst = parseInstant(from);
        Instant toInst = parseInstant(to);
        validateRange(fromInst, toInst);
        int safeLimit = limit == null ? DEFAULT_EVENT_LIMIT : Math.min(Math.max(1, limit), MAX_EVENT_LIMIT);

        try {
            return deviceStatusEventService.getEvents(compositeId, fromInst, toInst, safeLimit);
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage(), iae);
        }
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            long epoch = Long.parseLong(value.trim());
            return Instant.ofEpochMilli(epoch);
        } catch (NumberFormatException ignore) {
            // Fall through.
        }
        try {
            return Instant.parse(value.trim());
        } catch (Exception ignore) {
            return null;
        }
    }

    private static void validateRange(Instant from, Instant to) {
        if (from == null || to == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid 'from' or 'to' timestamp");
        }
        if (!to.isAfter(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'to' must be after 'from'");
        }
    }
}
