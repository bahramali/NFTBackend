package se.hydroleaf.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Spy;
import se.hydroleaf.dto.LiveNowSnapshot;
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
        when(oxygenPumpStatusRepository.getLatestPumpAverage("Sys", "Layer"))
                .thenReturn(avg);

        StatusAverageResponse response = statusService.getAverage("Sys", "Layer", "airpump");
        assertEquals(1.5, response.average());
        assertEquals(2L, response.deviceCount());
        verify(oxygenPumpStatusRepository).getLatestPumpAverage("Sys", "Layer");
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
        when(oxygenPumpStatusRepository.getLatestPumpAverage("sys", "layer"))
                .thenReturn(simpleResult(5.0, 5L));

        StatusAllAverageResponse response = statusService.getAllAverages("sys", "layer");

        assertEquals(1.0, response.light().average());
        assertEquals(2.0, response.humidity().average());
        assertEquals(3.0, response.temperature().average());
        assertEquals(4.0, response.dissolvedOxygen().average());
        assertEquals(5.0, response.airpump().average());
    }

    @Test
    void getLiveNowSnapshotAggregatesByDevice() {
        Device d1 = Device.builder().compositeId("1").system("S01").layer("L01").build();
        Device d2 = Device.builder().compositeId("2").system("S01").layer("L02").build();
        Device d3 = Device.builder().compositeId("3").system("S02").layer("L01").build();
        when(deviceRepository.findAll()).thenReturn(java.util.List.of(d1, d2, d3));

        StatusAverageResponse pump = new StatusAverageResponse(1.0, 1L);
        StatusAverageResponse light = new StatusAverageResponse(2.0, 2L);
        StatusAverageResponse humidity = new StatusAverageResponse(3.0, 3L);
        StatusAverageResponse temp = new StatusAverageResponse(4.0, 4L);
        StatusAverageResponse dox = new StatusAverageResponse(5.0, 5L);

        StatusAllAverageResponse r1 = new StatusAllAverageResponse(light, humidity, temp, dox, pump);
        StatusAllAverageResponse r2 = new StatusAllAverageResponse(light, humidity, temp, dox, pump);
        StatusAllAverageResponse r3 = new StatusAllAverageResponse(light, humidity, temp, dox, pump);

        doReturn(r1).when(statusService).getAllAverages("S01", "L01");
        doReturn(r2).when(statusService).getAllAverages("S01", "L02");
        doReturn(r3).when(statusService).getAllAverages("S02", "L01");

        LiveNowSnapshot result = statusService.getLiveNowSnapshot();

        assertEquals(pump, result.systems().get("S01").get("L01").actuator().airPump());
        assertEquals(light, result.systems().get("S01").get("L02").growSensors().light());
        assertEquals(dox, result.systems().get("S02").get("L01").waterTank().dissolvedOxygen());

        verify(statusService).getAllAverages("S01", "L01");
        verify(statusService).getAllAverages("S01", "L02");
        verify(statusService).getAllAverages("S02", "L01");
    }

    @Test
    void getLiveNowSnapshotSkipsBlankSystemOrLayer() {
        Device d1 = Device.builder().compositeId("1").system("").layer("L01").build();
        Device d2 = Device.builder().compositeId("2").system("S01").layer(" ").build();
        Device d3 = Device.builder().compositeId("3").system(null).layer("L02").build();
        Device d4 = Device.builder().compositeId("4").system("S01").layer(null).build();
        Device valid = Device.builder().compositeId("5").system("S01").layer("L01").build();
        when(deviceRepository.findAll()).thenReturn(java.util.List.of(d1, d2, d3, d4, valid));

        StatusAllAverageResponse r = new StatusAllAverageResponse(null, null, null, null, null);
        doReturn(r).when(statusService).getAllAverages("S01", "L01");

        LiveNowSnapshot result = statusService.getLiveNowSnapshot();

        assertEquals(1, result.systems().size());
        assertEquals(1, result.systems().get("S01").size());
        assertEquals(r.airpump(), result.systems().get("S01").get("L01").actuator().airPump());
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
