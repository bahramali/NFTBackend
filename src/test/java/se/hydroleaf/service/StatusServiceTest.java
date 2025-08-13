package se.hydroleaf.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Spy;
import se.hydroleaf.dto.StatusAllAverageResponse;
import se.hydroleaf.dto.StatusAverageResponse;
import se.hydroleaf.model.Device;
import se.hydroleaf.repository.AverageResult;
import se.hydroleaf.repository.DeviceRepository;
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

    @Mock
    private DeviceRepository deviceRepository;

    @Spy
    @InjectMocks
    private StatusService statusService;

    @Test
    void getAverageUsesSensorDataRepository() {
        AverageResult avg = simpleResult(10.0, 3L);
        when(sensorDataRepository.getLatestAverage("Sys", "Layer", "light"))
                .thenReturn(avg);

        StatusAverageResponse response = statusService.getAverage("Sys", "Layer", "light");
        assertEquals(10.0, response.average());
        assertEquals(3L, response.deviceCount());
        verify(sensorDataRepository).getLatestAverage("Sys", "Layer", "light");
        verifyNoInteractions(oxygenPumpStatusRepository);
    }

    @Test
    void getAverageUsesOxygenPumpRepositoryForAirpump() {
        AverageResult avg = simpleResult(1.5, 2L);
        when(oxygenPumpStatusRepository.getLatestAverage("Sys", "Layer"))
                .thenReturn(avg);

        StatusAverageResponse response = statusService.getAverage("Sys", "Layer", "airpump");
        assertEquals(1.5, response.average());
        assertEquals(2L, response.deviceCount());
        verify(oxygenPumpStatusRepository).getLatestAverage("Sys", "Layer");
        verifyNoMoreInteractions(oxygenPumpStatusRepository);
        verifyNoInteractions(sensorDataRepository);
    }

    @Test
    void getAllAveragesAggregatesAllSensorTypes() {
        when(sensorDataRepository.getLatestAverage("sys", "layer", "light"))
                .thenReturn(simpleResult(1.0, 1L));
        when(sensorDataRepository.getLatestAverage("sys", "layer", "humidity"))
                .thenReturn(simpleResult(2.0, 2L));
        when(sensorDataRepository.getLatestAverage("sys", "layer", "temperature"))
                .thenReturn(simpleResult(3.0, 3L));
        when(sensorDataRepository.getLatestAverage("sys", "layer", "dissolvedOxygen"))
                .thenReturn(simpleResult(4.0, 4L));
        when(oxygenPumpStatusRepository.getLatestAverage("sys", "layer"))
                .thenReturn(simpleResult(5.0, 5L));

        StatusAllAverageResponse response = statusService.getAllAverages("sys", "layer");

        assertEquals(1.0, response.light().average());
        assertEquals(2.0, response.humidity().average());
        assertEquals(3.0, response.temperature().average());
        assertEquals(4.0, response.dissolvedOxygen().average());
        assertEquals(5.0, response.airpump().average());
    }

    @Test
    void getAllSystemLayerAveragesAggregatesByDevice() {
        Device d1 = Device.builder().id("1").system("S01").layer("L01").build();
        Device d2 = Device.builder().id("2").system("S01").layer("L02").build();
        Device d3 = Device.builder().id("3").system("S02").layer("L01").build();
        when(deviceRepository.findAll()).thenReturn(java.util.List.of(d1, d2, d3));

        StatusAllAverageResponse r1 = new StatusAllAverageResponse(null, null, null, null, null);
        StatusAllAverageResponse r2 = new StatusAllAverageResponse(null, null, null, null, null);
        StatusAllAverageResponse r3 = new StatusAllAverageResponse(null, null, null, null, null);

        doReturn(r1).when(statusService).getAllAverages("S01", "L01");
        doReturn(r2).when(statusService).getAllAverages("S01", "L02");
        doReturn(r3).when(statusService).getAllAverages("S02", "L01");

        var result = statusService.getAllSystemLayerAverages();

        assertEquals(r1, result.get("S01").get("L01"));
        assertEquals(r2, result.get("S01").get("L02"));
        assertEquals(r3, result.get("S02").get("L01"));

        verify(statusService).getAllAverages("S01", "L01");
        verify(statusService).getAllAverages("S01", "L02");
        verify(statusService).getAllAverages("S02", "L01");
    }

    @Test
    void getAllSystemLayerAveragesSkipsBlankSystemOrLayer() {
        Device d1 = Device.builder().id("1").system("").layer("L01").build();
        Device d2 = Device.builder().id("2").system("S01").layer(" ").build();
        Device d3 = Device.builder().id("3").system(null).layer("L02").build();
        Device valid = Device.builder().id("4").system("S01").layer("L01").build();
        when(deviceRepository.findAll()).thenReturn(java.util.List.of(d1, d2, d3, valid));

        StatusAllAverageResponse r = new StatusAllAverageResponse(null, null, null, null, null);
        doReturn(r).when(statusService).getAllAverages("S01", "L01");
        doReturn(r).when(statusService).getAllAverages("S01", " ");

        var result = statusService.getAllSystemLayerAverages();

        assertEquals(1, result.size());
        assertEquals(r, result.get("S01").get("L01"));
        verify(statusService).getAllAverages("S01", "L01");
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
