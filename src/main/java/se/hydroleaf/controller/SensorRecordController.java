package se.hydroleaf.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.model.SensorRecord;
import se.hydroleaf.service.RecordService;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/sensors")
public class SensorRecordController {

    private final RecordService recordService;

    public SensorRecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping("/history")
    public List<SensorRecord> getHistory(
            @RequestParam("espId") String espId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return recordService.getRecords(espId, from, to);
    }
}
