package se.hydroleaf.service;


import org.springframework.stereotype.Component;
import se.hydroleaf.repository.SensorAggregationRepository;
import se.hydroleaf.util.InstantUtil;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class SensorAggregationAdapter implements RecordService.SensorAggregationReader {

    private final SensorAggregationRepository repo;

    public SensorAggregationAdapter(SensorAggregationRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<RecordService.SensorAggregateResult> aggregate(String compositeId,
                                                               Instant from,
                                                               Instant to,
                                                               String bucket) {
        long sec = InstantUtil.bucketSeconds(bucket);

        var rows = repo.aggregate(compositeId, from, to, sec);
        List<RecordService.SensorAggregateResult> out = new ArrayList<>(rows.size());

        for (SensorAggregationRepository.Row r : rows) {
            out.add(new RowImpl(
                    r.getSensorType(),
                    r.getUnit(),
                    r.getBucketTime(),
                    r.getAvgValue()
            ));
        }
        return out;
    }

    // Simple DTO implementing the service's projection interface
    private record RowImpl(
            String sensorType,
            String unit,
            Instant bucketTime,
            Double avgValue
    ) implements RecordService.SensorAggregateResult {
        @Override public String getSensorType() { return sensorType; }
        @Override public String getUnit() { return unit; }
        @Override public Instant getBucketTime() { return bucketTime; }
        @Override public Double getAvgValue() { return avgValue; }
    }
}