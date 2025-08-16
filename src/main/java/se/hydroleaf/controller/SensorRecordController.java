package se.hydroleaf.controller;


import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.dto.history.AggregatedHistoryResponse;
import se.hydroleaf.service.RecordService;

import java.time.Instant;

/**
 * SensorRecordController aligned with the new RecordService API.
 * - POST /api/records/{compositeId} : persist a record (JSON body)
 * - GET  /api/records/history/aggregated?compositeId=...&from=...&to=...&bucket=5m
 *   returns AggregatedHistoryResponse
 *
 * Comments are in English only.
 */
@RestController
@RequestMapping("/api/records")
public class SensorRecordController {

    private final RecordService recordService;

    public SensorRecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    /** Persist a record for the given compositeId. */
    @PostMapping(path = "/{compositeId}", consumes = "application/json")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void saveRecord(@PathVariable String compositeId, @RequestBody JsonNode body) {
        try {
            recordService.saveRecord(compositeId, body);
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage(), iae);
        } catch (RuntimeException re) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to persist record", re);
        }
    }

    /** Aggregated history by time bucket. Accepts ISO-8601 or epoch millis for 'from'/'to'.
     *  Optionally filters by sensorType.
     */
    @GetMapping("/history/aggregated")
    public AggregatedHistoryResponse getHistoryAggregated(
            @RequestParam("compositeId") String compositeId,
            @RequestParam("from") String from,
            @RequestParam("to") String to,
            @RequestParam(name = "bucket", defaultValue = "5m") String bucket,
            @RequestParam(name = "sensorType", required = false) String sensorType
    ) {
        Instant fromInst = parseInstant(from);
        Instant toInst = parseInstant(to);
        if (fromInst == null || toInst == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid 'from' or 'to' timestamp");
        }
        if (!toInst.isAfter(fromInst)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'to' must be after 'from'");
        }
        try {
            return recordService.aggregatedHistory(compositeId, fromInst, toInst, bucket, sensorType);
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage(), iae);
        }
    }

    // -------- helpers --------

    private static Instant parseInstant(String s) {
        if (s == null || s.isBlank()) return null;
        // epoch millis
        try {
            long epoch = Long.parseLong(s.trim());
            return Instant.ofEpochMilli(epoch);
        } catch (NumberFormatException ignore) {}
        // ISO-8601
        try {
            return Instant.parse(s.trim());
        } catch (Exception ignore) {}
        return null;
    }
}
