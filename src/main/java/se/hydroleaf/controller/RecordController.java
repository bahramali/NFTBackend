package se.hydroleaf.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.repository.dto.history.AggregatedHistoryResponse;
import se.hydroleaf.service.AuthorizationService;
import se.hydroleaf.service.RecordService;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/records")
public class RecordController {

    private static final Logger log = LoggerFactory.getLogger(RecordController.class);

    private static final long MAX_RANGE_DAYS = 31;
    private static final Pattern SENSOR_TYPE_PATTERN = Pattern.compile("[A-Za-z0-9_\\-]{1,32}");

    private final RecordService recordService;
    private final AuthorizationService authorizationService;

    @Autowired
    public RecordController(RecordService recordService, AuthorizationService authorizationService) {
        this.recordService = recordService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/history/aggregated")
    public AggregatedHistoryResponse getHistoryAggregated(
            @RequestHeader(name = "Authorization", required = false) String token,
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
        authorizationService.requireAdminOrOperator(token);
        if (log.isDebugEnabled()) {
            log.debug("Aggregated history request: compositeId={} from={} to={} bucket={} sensorTypes={} bucketLimit={} bucketOffset={} sensorLimit={} sensorOffset={} tokenPresent={}",
                    compositeId, from, to, bucket, sensorTypes, bucketLimit, bucketOffset, sensorLimit, sensorOffset, token != null);
        }
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
        List<String> normalizedSensorTypes = null;
        if (sensorTypes != null) {
            normalizedSensorTypes = new ArrayList<>(sensorTypes.size());
            for (String st : sensorTypes) {
                if (st == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sensorType");
                }
                String trimmed = st.trim();
                if (!SENSOR_TYPE_PATTERN.matcher(trimmed).matches()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sensorType");
                }
                normalizedSensorTypes.add(trimmed);
            }
        }
        try {
            return recordService.aggregatedHistory(
                    compositeId, fromInst, toInst, bucket, normalizedSensorTypes,
                    bucketLimit, bucketOffset, sensorLimit, sensorOffset);
        } catch (IllegalArgumentException iae) {
            if (log.isDebugEnabled()) {
                log.debug("Aggregated history validation error: {}", iae.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage(), iae);
        }
    }

    @PostMapping("/history/aggregated")
    public AggregatedHistoryResponse postHistoryAggregated(
            @RequestHeader(name = "Authorization", required = false) String token,
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
        authorizationService.requireAdminOrOperator(token);
        return getHistoryAggregated(
                token,
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
