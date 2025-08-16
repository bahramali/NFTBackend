package se.hydroleaf.service;


import org.springframework.stereotype.Component;
import se.hydroleaf.repository.SensorAggregationRepository;
import se.hydroleaf.repository.TimescaleDbSupport;
import se.hydroleaf.repository.dto.SensorAggregationRow;
import se.hydroleaf.util.InstantUtil;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class SensorAggregationAdapter implements RecordService.SensorAggregationReader {

    private final SensorAggregationRepository repo;
    private final TimescaleDbSupport timescale;

    public SensorAggregationAdapter(SensorAggregationRepository repo, TimescaleDbSupport timescale) {
        this.repo = repo;
        this.timescale = timescale;
    }

    @Override
    public List<RecordService.SensorAggregateResult> aggregate(String compositeId,
                                                               Instant from,
                                                               Instant to,
                                                               String bucket,
                                                               String sensorType) {
        long sec = InstantUtil.bucketSeconds(bucket);

        var rows = timescale.isAvailable()
                ? repo.aggregateTimescale(compositeId, from, to, sec, sensorType)
                : repo.aggregateDateTrunc(compositeId, from, to, sec, sensorType);
        List<RecordService.SensorAggregateResult> out = new ArrayList<>(rows.size());

        for (SensorAggregationRow r : rows) {
            out.add(new RowImpl(
                    r.sensorType(),
                    r.unit(),
                    r.bucketTime(),
                    r.avgValue()
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