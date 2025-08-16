package se.hydroleaf.dto.history;

import java.time.Instant;
import java.util.List;

public record AggregatedHistoryResponse(
        Instant fromDate,
        Instant toDate,
        List<AggregatedSensorData> sensors
) {}
