package se.hydroleaf.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.hydroleaf.repository.ActuatorStatusRepository;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.LatestSensorValueRepository;
import se.hydroleaf.repository.dto.history.AggregatedHistoryResponse;

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
        when(latestSensorValueRepository.findByDevice_CompositeId(eq(DEVICE_ID)))
                .thenReturn(Collections.emptyList());

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

    private static final class CapturingAggregationReader implements RecordService.SensorAggregationReader {

        Instant capturedFrom;
        Instant capturedTo;
        String capturedBucket;
        String capturedSensorType;

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
            return Collections.emptyList();
        }
    }
}
