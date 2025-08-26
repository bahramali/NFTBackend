package se.hydroleaf.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.repository.dto.history.AggregatedHistoryResponse;
import se.hydroleaf.service.RecordService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/records")
public class RecordController {

    private static final long MAX_RANGE_DAYS = 31;
    private static final Pattern SENSOR_TYPE_PATTERN = Pattern.compile("[A-Za-z0-9_\\-]{1,32}");

    private final RecordService recordService;

    @Autowired
    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping("/history/aggregated")
    public AggregatedHistoryResponse getHistoryAggregated(
            @RequestParam("compositeId") String compositeId,
            @RequestParam("from") String from,
            @RequestParam("to") String to,
            @RequestParam(name = "bucket", defaultValue = "5m") String bucket,
            @RequestParam(name = "sensorType", required = false) List<String> sensorTypes,
            @RequestParam(name = "bucketLimit", required = false) Integer bucketLimit,
            @RequestParam(name = "bucketOffset", required = false) Integer bucketOffset,
            @RequestParam(name = "sensorLimit", required = false) Integer sensorLimit,
            @RequestParam(name = "sensorOffset", required = false) Integer sensorOffset
    ) {
        Instant fromInst = parseInstant(from);
        Instant toInst = parseInstant(to);
        if (fromInst == null || toInst == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid 'from' or 'to' timestamp");
        }
        if (!toInst.isAfter(fromInst)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'to' must be after 'from'");
        }
        if (Duration.between(fromInst, toInst).toDays() > MAX_RANGE_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Range exceeds " + MAX_RANGE_DAYS + " days");
        }
        if (sensorTypes != null) {
            for (String st : sensorTypes) {
                if (!SENSOR_TYPE_PATTERN.matcher(st).matches()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sensorType");
                }
            }
        }
        try {
            return recordService.aggregatedHistory(
                    compositeId, fromInst, toInst, bucket, sensorTypes,
                    bucketLimit, bucketOffset, sensorLimit, sensorOffset);
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage(), iae);
        }
    }

    @PostMapping("/history/aggregated")
    public AggregatedHistoryResponse postHistoryAggregated(
            @RequestParam("compositeId") String compositeId,
            @RequestParam("from") String from,
            @RequestParam("to") String to,
            @RequestParam(name = "bucket", defaultValue = "5m") String bucket,
            @RequestParam(name = "sensorType", required = false) List<String> sensorTypes,
            @RequestParam(name = "bucketLimit", required = false) Integer bucketLimit,
            @RequestParam(name = "bucketOffset", required = false) Integer bucketOffset,
            @RequestParam(name = "sensorLimit", required = false) Integer sensorLimit,
            @RequestParam(name = "sensorOffset", required = false) Integer sensorOffset
    ) {
        return getHistoryAggregated(
                compositeId,
                from,
                to,
                bucket,
                sensorTypes,
                bucketLimit,
                bucketOffset,
                sensorLimit,
                sensorOffset);
    }

    private static Instant parseInstant(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            long epoch = Long.parseLong(s.trim());
            return Instant.ofEpochMilli(epoch);
        } catch (NumberFormatException ignore) {}
        try {
            return Instant.parse(s.trim());
        } catch (Exception ignore) {}
        return null;
    }
}
