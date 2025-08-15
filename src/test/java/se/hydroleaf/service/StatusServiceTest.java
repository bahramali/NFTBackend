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
import se.hydroleaf.dto.SystemSnapshot;
import se.hydroleaf.model.Device;
import se.hydroleaf.repository.ActuatorStatusRepository;
import se.hydroleaf.repository.AverageResult;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.SensorDataRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        assertEquals("lux", response.unit());
        assertEquals(3L, response.deviceCount());
        verify(sensorDataRepository).getLatestAverage("Sys", "Layer", "light");
        verifyNoInteractions(actuatorStatusRepository);
    }

    @Test
    void getAverageUsesSensorDataRepositoryForWaterTankSensor() {
        AverageResult avg = simpleResult(9.9, 4L);
        when(sensorDataRepository.getLatestAverage("Sys", "Layer", "dissolvedOxygen"))
                .thenReturn(avg);

        StatusAverageResponse response = statusService.getAverage("Sys", "Layer", "dissolvedOxygen");
        assertEquals(9.9, response.average());
        assertEquals("mg/L", response.unit());
        assertEquals(4L, response.deviceCount());
        verify(sensorDataRepository).getLatestAverage("Sys", "Layer", "dissolvedOxygen");
        verifyNoInteractions(actuatorStatusRepository);
    }

    @Test
    void getAverageUsesActuatorRepositoryForAirpump() {
        AverageResult avg = simpleResult(1.5, 2L);
        when(actuatorStatusRepository.getLatestActuatorAverage("Sys", "Layer", "airpump"))
                .thenReturn(avg);

        StatusAverageResponse response = statusService.getAverage("Sys", "Layer", "airpump");
        assertEquals(1.5, response.average());
        assertEquals("status", response.unit());
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
        when(sensorDataRepository.getLatestAverage("sys", "layer", "airTemperature"))
                .thenReturn(simpleResult(3.0, 3L));
        when(sensorDataRepository.getLatestAverage("sys", "layer", "dissolvedOxygen"))
                .thenReturn(simpleResult(4.0, 4L));
        when(sensorDataRepository.getLatestAverage("sys", "layer", "dissolvedTemp"))
                .thenReturn(simpleResult(5.0, 5L));
        when(sensorDataRepository.getLatestAverage("sys", "layer", "pH"))
                .thenReturn(simpleResult(6.0, 6L));
        when(sensorDataRepository.getLatestAverage("sys", "layer", "dissolvedEC"))
                .thenReturn(simpleResult(7.0, 7L));
        when(sensorDataRepository.getLatestAverage("sys", "layer", "dissolvedTDS"))
                .thenReturn(simpleResult(8.0, 8L));
        when(actuatorStatusRepository.getLatestActuatorAverage("sys", "layer", "airPump"))
                .thenReturn(simpleResult(9.0, 9L));

        StatusAllAverageResponse response = statusService.getAllAverages("sys", "layer");

        assertEquals(1.0, response.growSensors().get("light").average());
        assertEquals(2.0, response.growSensors().get("humidity").average());
        assertEquals(3.0, response.growSensors().get("airTemperature").average());
        assertEquals(5.0, response.waterTank().get("dissolvedTemp").average());
        assertEquals(4.0, response.waterTank().get("dissolvedOxygen").average());
        assertEquals(6.0, response.waterTank().get("pH").average());
        assertEquals(7.0, response.waterTank().get("dissolvedEC").average());
        assertEquals(8.0, response.waterTank().get("dissolvedTDS").average());
        assertEquals(9.0, response.airpump().average());

        verify(sensorDataRepository).getLatestAverage("sys", "layer", "dissolvedTemp");
        verify(sensorDataRepository).getLatestAverage("sys", "layer", "pH");
        verify(sensorDataRepository).getLatestAverage("sys", "layer", "dissolvedEC");
        verify(sensorDataRepository).getLatestAverage("sys", "layer", "dissolvedTDS");
    }

    @Test
    void getLiveNowSnapshotAggregatesByDevice() {
        Device d1 = Device.builder().compositeId("1").system("S01").layer("L01").build();
        Device dDup = Device.builder().compositeId("3").system("S01").layer("L01").build();
        Device d2 = Device.builder().compositeId("2").system("S02").layer("L01").build();
        when(deviceRepository.findAll()).thenReturn(java.util.List.of(d1, dDup, d2));

        StatusAverageResponse pump = new StatusAverageResponse(1.0, "status",1L);
        StatusAverageResponse light = new StatusAverageResponse(2.0, "lux",2L);
        StatusAverageResponse humidity = new StatusAverageResponse(3.0, "%",3L);
        StatusAverageResponse temp = new StatusAverageResponse(4.0, "°C",4L);
        StatusAverageResponse dTemp = new StatusAverageResponse(5.0, "°C",5L);
        StatusAverageResponse dOxy = new StatusAverageResponse(6.0, "mg/L",6L);
        StatusAverageResponse dPh = new StatusAverageResponse(7.0, "pH",7L);
        StatusAverageResponse dEc = new StatusAverageResponse(8.0, "mS/cm",8L);
        StatusAverageResponse dTds = new StatusAverageResponse(9.0, "ppm",9L);

        doAnswer(invocation -> {
            String type = invocation.getArgument(2);
            return switch (type) {
                case "airPump" -> pump;
                case "light" -> light;
                case "humidity" -> humidity;
                case "airTemperature" -> temp;
                case "dissolvedTemp" -> dTemp;
                case "dissolvedOxygen" -> dOxy;
                case "pH" -> dPh;
                case "dissolvedEC" -> dEc;
                case "dissolvedTDS" -> dTds;
                default -> null;
            };
        }).when(statusService).getAverage(anyString(), anyString(), anyString());

        LiveNowSnapshot result = statusService.getLiveNowSnapshot();
        SystemSnapshot s01System = result.systems().get("S01");
        List<SystemSnapshot.LayerSnapshot> s01Layers = s01System.layers();
        assertEquals(1, s01Layers.size());
        SystemSnapshot.LayerSnapshot s01Layer = s01Layers.get(0);
        SystemSnapshot.LayerSnapshot s02Layer = result.systems().get("S02").layers().get(0);
        assertEquals(pump, s01Layer.actuators().airPump());
        assertEquals(light, s01Layer.environment().light());
        assertEquals(dOxy, s02Layer.water().dissolvedOxygen());
        assertNotNull(s01Layer.lastUpdate());
        assertEquals(pump, s01System.actuators().airPump());
        assertEquals(light, s01System.environment().light());
        assertEquals(dOxy, result.systems().get("S02").water().dissolvedOxygen());

        verify(statusService, times(1)).getAverage("S01", "L01", "airPump");
        verify(statusService, times(1)).getAverage("S01", "L01", "light");
        verify(statusService, atLeastOnce()).getAverage("S02", "L01", "dissolvedOxygen");
    }

    @Test
    void getLiveNowSnapshotAggregatesMultipleLayers() {
        Device d1 = Device.builder().compositeId("1").system("S01").layer("L01").build();
        Device d2 = Device.builder().compositeId("2").system("S01").layer("L02").build();
        when(deviceRepository.findAll()).thenReturn(java.util.List.of(d1, d2));

        StatusAverageResponse pumpL1 = new StatusAverageResponse(1.0, "status",1L);
        StatusAverageResponse pumpL2 = new StatusAverageResponse(3.0, "status",1L);
        StatusAverageResponse waterL1 = new StatusAverageResponse(10.0, "°C",1L);
        StatusAverageResponse waterL2 = new StatusAverageResponse(30.0, "°C",1L);

        doAnswer(invocation -> {
            String layer = invocation.getArgument(1);
            String type = invocation.getArgument(2);
            return switch (layer + type) {
                case "L01airPump" -> pumpL1;
                case "L02airPump" -> pumpL2;
                case "L01dissolvedTemp" -> waterL1;
                case "L02dissolvedTemp" -> waterL2;
                default -> new StatusAverageResponse(null, null,0L);
            };
        }).when(statusService).getAverage(anyString(), anyString(), anyString());

        LiveNowSnapshot snapshot = statusService.getLiveNowSnapshot();

        SystemSnapshot system = snapshot.systems().get("S01");
        SystemSnapshot.LayerSnapshot layer1 = system.layers().stream()
                .filter(l -> l.layerId().equals("L01"))
                .findFirst().orElseThrow();
        SystemSnapshot.LayerSnapshot layer2 = system.layers().stream()
                .filter(l -> l.layerId().equals("L02"))
                .findFirst().orElseThrow();

        assertEquals(2.0, system.actuators().airPump().average());
        assertEquals(20.0, system.water().dissolvedTemp().average());
        assertEquals(layer2.lastUpdate(), system.lastUpdate());
        assertEquals(pumpL1, layer1.actuators().airPump());
        assertEquals(waterL2, layer2.water().dissolvedTemp());
    }

    @Test
    void getLiveNowSnapshotSkipsBlankSystemOrLayer() {
        Device d1 = Device.builder().compositeId("1").system("").layer("L01").build();
        Device d2 = Device.builder().compositeId("2").system("S01").layer(" ").build();
        Device d3 = Device.builder().compositeId("3").system(null).layer("L02").build();
        Device d4 = Device.builder().compositeId("4").system("S01").layer(null).build();
        Device valid = Device.builder().compositeId("5").system("S01").layer("L01").build();
        when(deviceRepository.findAll()).thenReturn(java.util.List.of(d1, d2, d3, d4, valid));

        StatusAverageResponse pump = new StatusAverageResponse(1.0, "status",1L);
        doAnswer(invocation -> {
            String type = invocation.getArgument(2);
            return "airPump".equals(type) ? pump : new StatusAverageResponse(null, null,0L);
        }).when(statusService).getAverage(anyString(), anyString(), anyString());

        LiveNowSnapshot result = statusService.getLiveNowSnapshot();

        assertEquals(1, result.systems().size());
        SystemSnapshot system = result.systems().get("S01");
        List<SystemSnapshot.LayerSnapshot> layers = system.layers();
        assertEquals(1, layers.size());
        SystemSnapshot.LayerSnapshot layerSnapshot = layers.get(0);
        assertEquals(pump, layerSnapshot.actuators().airPump());
        assertNotNull(layerSnapshot.lastUpdate());
        assertEquals(pump, system.actuators().airPump());
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
