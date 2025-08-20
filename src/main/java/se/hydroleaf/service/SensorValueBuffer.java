package se.hydroleaf.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import se.hydroleaf.model.SensorValueHistory;
import se.hydroleaf.repository.SensorValueHistoryRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class SensorValueBuffer {

    private record Key(String compositeId, String sensorType) { }

    private static class Accumulator {
        double sum;
        long count;
        Instant firstTimestamp;

        void accumulate(double value, Instant ts) {
            sum += value;
            count++;
            if (firstTimestamp == null || ts.isBefore(firstTimestamp)) {
                firstTimestamp = ts;
            }
        }
    }

    private final ConcurrentMap<Key, Accumulator> buffer = new ConcurrentHashMap<>();
    private final SensorValueHistoryRepository sensorValueHistoryRepository;

    public SensorValueBuffer(SensorValueHistoryRepository sensorValueHistoryRepository) {
        this.sensorValueHistoryRepository = sensorValueHistoryRepository;
    }

    public void add(String compositeId, String sensorType, double value, Instant timestamp) {
        Key key = new Key(compositeId, sensorType);
        buffer.compute(key, (k, acc) -> {
            if (acc == null) {
                acc = new Accumulator();
            }
            acc.accumulate(value, timestamp);
            return acc;
        });
    }

    @Scheduled(fixedRate = 60000, scheduler = "scheduler")
    public void flush() {
        Map<Key, Accumulator> snapshot = drain();
        if (snapshot.isEmpty()) {
            return;
        }
        List<SensorValueHistory> history = new ArrayList<>(snapshot.size());
        for (Map.Entry<Key, Accumulator> e : snapshot.entrySet()) {
            Accumulator a = e.getValue();
            double avg = a.sum / a.count;
            history.add(SensorValueHistory.builder()
                    .compositeId(e.getKey().compositeId())
                    .sensorType(e.getKey().sensorType())
                    .sensorValue(avg)
                    .valueTime(a.firstTimestamp)
                    .build());
        }
        sensorValueHistoryRepository.saveAll(history);
    }

    private Map<Key, Accumulator> drain() {
        Map<Key, Accumulator> snapshot = new HashMap<>();
        buffer.forEach((k, v) -> {
            if (buffer.remove(k, v)) {
                snapshot.put(k, v);
            }
        });
        return snapshot;
    }
}
