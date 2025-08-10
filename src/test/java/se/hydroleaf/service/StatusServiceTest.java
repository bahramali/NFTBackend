package se.hydroleaf.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.hydroleaf.dto.StatusAllAverageResponse;
import se.hydroleaf.dto.StatusAverageResponse;
import se.hydroleaf.repository.AverageResult;
import se.hydroleaf.repository.OxygenPumpStatusRepository;
import se.hydroleaf.repository.SensorDataRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatusServiceTest {

    @Mock
    private SensorDataRepository sensorDataRepository;

    @Mock
    private OxygenPumpStatusRepository oxygenPumpStatusRepository;

    @InjectMocks
    private StatusService statusService;

    @Test
    void getAverageUsesSensorDataRepository() {
        AverageResult avg = simpleResult(10.0, 3L);
        when(sensorDataRepository.getLatestAverage("sys", "layer", "lux"))
                .thenReturn(avg);

        StatusAverageResponse response = statusService.getAverage("sys", "layer", "lux");
        assertEquals(10.0, response.average());
        assertEquals(3L, response.deviceCount());
        verify(sensorDataRepository).getLatestAverage("sys", "layer", "lux");
        verifyNoInteractions(oxygenPumpStatusRepository);
    }

    @Test
    void getAverageUsesOxygenPumpRepositoryForAirpump() {
        AverageResult avg = simpleResult(1.5, 2L);
        when(oxygenPumpStatusRepository.getLatestAverage("sys", "layer"))
                .thenReturn(avg);

        StatusAverageResponse response = statusService.getAverage("sys", "layer", "airpump");
        assertEquals(1.5, response.average());
        assertEquals(2L, response.deviceCount());
        verify(oxygenPumpStatusRepository).getLatestAverage("sys", "layer");
        verifyNoMoreInteractions(oxygenPumpStatusRepository);
        verifyNoInteractions(sensorDataRepository);
    }

    @Test
    void getAllAveragesAggregatesAllSensorTypes() {
        when(sensorDataRepository.getLatestAverage("sys", "layer", "lux"))
                .thenReturn(simpleResult(1.0, 1L));
        when(sensorDataRepository.getLatestAverage("sys", "layer", "humidity"))
                .thenReturn(simpleResult(2.0, 2L));
        when(sensorDataRepository.getLatestAverage("sys", "layer", "temperature"))
                .thenReturn(simpleResult(3.0, 3L));
        when(sensorDataRepository.getLatestAverage("sys", "layer", "do"))
                .thenReturn(simpleResult(4.0, 4L));
        when(oxygenPumpStatusRepository.getLatestAverage("sys", "layer"))
                .thenReturn(simpleResult(5.0, 5L));

        StatusAllAverageResponse response = statusService.getAllAverages("sys", "layer");

        assertEquals(1.0, response.lux().average());
        assertEquals(2.0, response.humidity().average());
        assertEquals(3.0, response.temperature().average());
        assertEquals(4.0, response.dissolvedOxygen().average());
        assertEquals(5.0, response.airpump().average());
    }

    private AverageResult simpleResult(Double avg, Long count) {
        return new AverageResult() {
            @Override
            public Double getAverage() {
                return avg;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }
}
