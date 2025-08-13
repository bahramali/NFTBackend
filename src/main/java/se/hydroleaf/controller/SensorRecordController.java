package se.hydroleaf.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.dto.AggregatedHistoryResponse;
import se.hydroleaf.service.RecordService;
import se.hydroleaf.util.InstantUtil;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
@RestController
@RequestMapping("/api/sensors")
public class SensorRecordController {

    private final RecordService recordService;

    public SensorRecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping("/history/aggregated")
    public AggregatedHistoryResponse getHistoryAggregated(
            @RequestParam("compositeId") String compositeId,
            @RequestParam("from") String from,
            @RequestParam("to") String to) {
        Instant fromInst = InstantUtil.parse(from);
        Instant toInst = InstantUtil.parse(to);
        return recordService.getAggregatedRecords(compositeId, fromInst, toInst);
    }
}
