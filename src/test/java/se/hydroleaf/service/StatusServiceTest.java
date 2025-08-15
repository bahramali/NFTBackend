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
import se.hydroleaf.repository.ActuatorStatusRepository;
import se.hydroleaf.repository.AverageResult;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.SensorDataRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatusServiceTest {

    @Mock
    private SensorDataRepository sensorDataRepository;

    @Mock
    private ActuatorStatusRepository actuatorStatusRepository;

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
        verifyNoInteractions(actuatorStatusRepository);
    }

    @Test
    void getAverageUsesActuatorRepositoryForAirpump() {
        AverageResult avg = simpleResult(1.5, 2L);
        when(actuatorStatusRepository.getLatestActuatorAverage("Sys", "Layer", "airpump"))
                .thenReturn(avg);

        StatusAverageResponse response = statusService.getAverage("Sys", "Layer", "airpump");
        assertEquals(1.5, response.average());
        assertEquals(2L, response.deviceCount());
        verify(actuatorStatusRepository).getLatestActuatorAverage("Sys", "Layer", "airpump");
        verifyNoMoreInteractions(actuatorStatusRepository);
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
        when(actuatorStatusRepository.getLatestActuatorAverage("sys", "layer", "airPump"))
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
        StatusAverageResponse dTemp = new StatusAverageResponse(5.0, 5L);
        StatusAverageResponse dOxy = new StatusAverageResponse(6.0, 6L);
        StatusAverageResponse dPh = new StatusAverageResponse(7.0, 7L);
        StatusAverageResponse dEc = new StatusAverageResponse(8.0, 8L);

        doAnswer(invocation -> {
            String type = invocation.getArgument(2);
            return switch (type) {
                case "airPump" -> pump;
                case "light" -> light;
                case "humidity" -> humidity;
                case "temperature" -> temp;
                case "dissolvedTemp" -> dTemp;
                case "dissolvedOxygen" -> dOxy;
                case "dissolvedPH" -> dPh;
                case "dissolvedEC" -> dEc;
                default -> null;
            };
        }).when(statusService).getAverage(anyString(), anyString(), anyString());

        LiveNowSnapshot result = statusService.getLiveNowSnapshot();

        assertEquals(pump, result.systems().get("S01").get("L01").actuator().airPump());
        assertEquals(light, result.systems().get("S01").get("L02").growSensors().light());
        assertEquals(dOxy, result.systems().get("S02").get("L01").waterTank().dissolvedOxygen());

        verify(statusService, atLeastOnce()).getAverage("S01", "L01", "airPump");
        verify(statusService, atLeastOnce()).getAverage("S01", "L02", "light");
        verify(statusService, atLeastOnce()).getAverage("S02", "L01", "dissolvedOxygen");
    }

    @Test
    void getLiveNowSnapshotSkipsBlankSystemOrLayer() {
        Device d1 = Device.builder().compositeId("1").system("").layer("L01").build();
        Device d2 = Device.builder().compositeId("2").system("S01").layer(" ").build();
        Device d3 = Device.builder().compositeId("3").system(null).layer("L02").build();
        Device d4 = Device.builder().compositeId("4").system("S01").layer(null).build();
        Device valid = Device.builder().compositeId("5").system("S01").layer("L01").build();
        when(deviceRepository.findAll()).thenReturn(java.util.List.of(d1, d2, d3, d4, valid));

        StatusAverageResponse pump = new StatusAverageResponse(1.0, 1L);
        doAnswer(invocation -> {
            String type = invocation.getArgument(2);
            return "airPump".equals(type) ? pump : new StatusAverageResponse(null, 0L);
        }).when(statusService).getAverage(anyString(), anyString(), anyString());

        LiveNowSnapshot result = statusService.getLiveNowSnapshot();

        assertEquals(1, result.systems().size());
        assertEquals(1, result.systems().get("S01").size());
        assertEquals(pump, result.systems().get("S01").get("L01").actuator().airPump());
        verify(statusService, atLeastOnce()).getAverage("S01", "L01", "airPump");
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
