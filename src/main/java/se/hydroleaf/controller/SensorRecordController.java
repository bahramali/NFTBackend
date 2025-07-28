package se.hydroleaf.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.dto.AggregatedHistoryResponse;
import se.hydroleaf.service.RecordService;

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
            @RequestParam("espId") String espId,
            @RequestParam("from") String from,
            @RequestParam("to") String to) {
        Instant fromInst = parseInstant(from);
        Instant toInst = parseInstant(to);
        return recordService.getAggregatedRecords(espId, fromInst, toInst);
    }

    private Instant parseInstant(String s) {
        try {
            return Instant.parse(s);
        } catch (DateTimeParseException e) {
            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd'T'H:mm:ss")
                    .appendOffsetId()
                    .toFormatter();
            return OffsetDateTime.parse(s, formatter).toInstant();
        }
    }
}
