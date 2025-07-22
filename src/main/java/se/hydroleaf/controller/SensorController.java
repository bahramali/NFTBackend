package se.hydroleaf.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.model.SensorRecord;
import se.hydroleaf.service.RecordService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sensors")
public class SensorController {

    private final RecordService recordService;

    public SensorController(RecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping("/latest")
    public SensorRecord latest() {
        return recordService.getLatestRecord().orElse(null);
    }

    @GetMapping("/history")
    public List<SensorRecord> history(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return recordService.getRecordsBetween(from, to);
    }

    @GetMapping("/statistics")
    public Map<String, Double> statistics(
            @RequestParam String type,
            @RequestParam String range) {
        Duration duration = parseDuration(range);
        return recordService.calculateStatistics(type, duration);
    }

    private Duration parseDuration(String range) {
        if (range.endsWith("h")) {
            long hours = Long.parseLong(range.substring(0, range.length() - 1));
            return Duration.ofHours(hours);
        } else if (range.endsWith("d")) {
            long days = Long.parseLong(range.substring(0, range.length() - 1));
            return Duration.ofDays(days);
        }
        throw new IllegalArgumentException("Unsupported range format: " + range);
    }
}
