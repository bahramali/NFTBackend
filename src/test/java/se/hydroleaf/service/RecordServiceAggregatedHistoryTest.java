package se.hydroleaf.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.hydroleaf.model.LatestSensorValue;
import se.hydroleaf.repository.ActuatorStatusRepository;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.LatestSensorValueRepository;
import se.hydroleaf.repository.dto.history.AggregatedHistoryResponse;
import se.hydroleaf.repository.dto.history.AggregatedSensorData;
import se.hydroleaf.repository.dto.history.TimestampValue;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordServiceAggregatedHistoryTest {

    private static final String DEVICE_ID = "dev-1";

    @Mock
    DeviceRepository deviceRepository;

    @Mock
    ActuatorStatusRepository actuatorStatusRepository;

    @Mock
    LatestSensorValueRepository latestSensorValueRepository;

    @Mock
    SensorValueBuffer sensorValueBuffer;

    private CapturingAggregationReader aggregationReader;
    private RecordService recordService;

    @BeforeEach
    void setUp() {
        aggregationReader = new CapturingAggregationReader();
        recordService = new RecordService(
                deviceRepository,
                actuatorStatusRepository,
                aggregationReader,
                latestSensorValueRepository,
                sensorValueBuffer
        );
    }

    @Test
    void aggregatedHistoryExtendsUpperBoundToCoverPartialBucket() {
        Instant from = Instant.parse("2023-11-01T00:03:00Z");
        Instant to = Instant.parse("2023-11-01T00:08:00Z");

        when(deviceRepository.existsById(eq(DEVICE_ID))).thenReturn(true);

        AggregatedHistoryResponse response = recordService.aggregatedHistory(
                DEVICE_ID,
                from,
                to,
                "5m",
                null
        );

        assertNotNull(response);
        assertEquals(Instant.parse("2023-11-01T00:00:00Z"), aggregationReader.capturedFrom);
        assertEquals(Instant.parse("2023-11-01T00:10:00Z"), aggregationReader.capturedTo);
        assertEquals("5m", aggregationReader.capturedBucket);
        assertNull(aggregationReader.capturedSensorType);
    }

    @Test
    void aggregatedHistoryResolvesSensorTypeCaseUsingLatestValues() {
        Instant from = Instant.parse("2023-11-01T00:00:00Z");
        Instant to = Instant.parse("2023-11-01T01:00:00Z");

        LatestSensorValue latest = new LatestSensorValue();
        latest.setSensorType("PPM");

        when(deviceRepository.existsById(eq(DEVICE_ID))).thenReturn(true);
        when(latestSensorValueRepository.findByDevice_CompositeId(eq(DEVICE_ID)))
                .thenReturn(List.of(latest));

        Instant bucketTime = Instant.parse("2023-11-01T00:05:00Z");
        aggregationReader.setResultsForSensorType("PPM", List.of(
                new TestAggregateResult("ppm", "ppm", bucketTime, 12.5d)
        ));

        AggregatedHistoryResponse response = recordService.aggregatedHistory(
                DEVICE_ID,
                from,
                to,
                "5m",
                List.of("ppm")
        );

        assertNotNull(response);
        assertEquals("PPM", aggregationReader.capturedSensorType);
        assertEquals(1, response.sensors().size());
        AggregatedSensorData sensorData = response.sensors().get(0);
        assertEquals("ppm", sensorData.sensorType());
        assertEquals("ppm", sensorData.unit());
        assertEquals(1, sensorData.data().size());
        TimestampValue value = sensorData.data().get(0);
        assertEquals(bucketTime, value.timestamp());
        assertEquals(12.5d, value.value());
    }

    private static final class CapturingAggregationReader implements RecordService.SensorAggregationReader {

        Instant capturedFrom;
        Instant capturedTo;
        String capturedBucket;
        String capturedSensorType;

        private final java.util.Map<String, List<RecordService.SensorAggregateResult>> resultsBySensorType = new java.util.HashMap<>();
        private List<RecordService.SensorAggregateResult> defaultResults = Collections.emptyList();

        void setResultsForSensorType(String sensorType, List<RecordService.SensorAggregateResult> results) {
            resultsBySensorType.put(sensorType, results);
        }

        void setDefaultResults(List<RecordService.SensorAggregateResult> results) {
            this.defaultResults = results;
        }

        @Override
        public List<RecordService.SensorAggregateResult> aggregate(String compositeId,
                                                                   Instant from,
                                                                   Instant to,
                                                                   String bucket,
                                                                   String sensorType) {
            this.capturedFrom = from;
            this.capturedTo = to;
            this.capturedBucket = bucket;
            this.capturedSensorType = sensorType;
            return resultsBySensorType.getOrDefault(sensorType, defaultResults);
        }
    }

    private record TestAggregateResult(String sensorType, String unit, Instant bucketTime, Double avgValue)
            implements RecordService.SensorAggregateResult {
        @Override public String getSensorType() { return sensorType; }
        @Override public String getUnit() { return unit; }
        @Override public Instant getBucketTime() { return bucketTime; }
        @Override public Double getAvgValue() { return avgValue; }
    }
}
