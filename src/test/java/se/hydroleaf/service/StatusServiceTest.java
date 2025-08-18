package se.hydroleaf.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.hydroleaf.dto.snapshot.LiveNowSnapshot;
import se.hydroleaf.dto.snapshot.SystemSnapshot;
import se.hydroleaf.dto.summary.StatusAllAverageResponse;
import se.hydroleaf.dto.summary.StatusAverageResponse;
import se.hydroleaf.repository.ActuatorStatusRepository;
import se.hydroleaf.repository.AverageCount;
import se.hydroleaf.repository.SensorDataRepository;
import se.hydroleaf.repository.dto.LiveNowRow;
import se.hydroleaf.model.DeviceType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class StatusServiceTest {

    @Mock
    private SensorDataRepository sensorDataRepository;

    @Mock
    private ActuatorStatusRepository actuatorStatusRepository;

    private StatusService statusService;

    @BeforeEach
    void setUp() {
        statusService = new StatusService(sensorDataRepository, actuatorStatusRepository);
    }

    @Test
    void getAverageUsesSensorDataRepository() {
        when(sensorDataRepository.getLatestAverage("Sys", "Layer", DeviceType.LIGHT))
                .thenReturn(new AverageCount(10.0, 1L));

        StatusAverageResponse response = statusService.getAverage("Sys", "Layer", "light");

        assertEquals(10.0, response.average());
        assertEquals("lux", response.unit());
        assertEquals(1L, response.deviceCount());
        verify(sensorDataRepository).getLatestAverage("Sys", "Layer", DeviceType.LIGHT);
        verifyNoInteractions(actuatorStatusRepository);
    }

    @Test
    void getAverageUsesSensorDataRepositoryForWaterTankSensor() {
        when(sensorDataRepository.getLatestAverage("Sys", "Layer", DeviceType.DISSOLVED_OXYGEN))
                .thenReturn(new AverageCount(9.9, 1L));

        StatusAverageResponse response = statusService.getAverage("Sys", "Layer", "dissolvedOxygen");

        assertEquals(9.9, response.average());
        assertEquals("mg/L", response.unit());
        assertEquals(1L, response.deviceCount());
        verify(sensorDataRepository).getLatestAverage("Sys", "Layer", DeviceType.DISSOLVED_OXYGEN);
        verifyNoInteractions(actuatorStatusRepository);
    }

    @Test
    void getAverageUsesActuatorStatusRepositoryForAirpump() {
        when(actuatorStatusRepository.getLatestActuatorAverage("Sys", "Layer", DeviceType.AIR_PUMP))
                .thenReturn(new AverageCount(1.0, 1L));

        StatusAverageResponse response = statusService.getAverage("Sys", "Layer", "airpump");

        assertEquals(1.0, response.average());
        assertEquals("status", response.unit());
        assertEquals(1L, response.deviceCount());
        verify(actuatorStatusRepository).getLatestActuatorAverage("Sys", "Layer", DeviceType.AIR_PUMP);
        verifyNoInteractions(sensorDataRepository);
    }

    @Test
    void getAllAveragesAggregatesAllSensorTypes() {
        Map<DeviceType, AverageCount> sensorMap = Map.of(
                DeviceType.LIGHT, new AverageCount(1.0, 1L),
                DeviceType.HUMIDITY, new AverageCount(2.0, 1L),
                DeviceType.TEMPERATURE, new AverageCount(3.0, 1L),
                DeviceType.DISSOLVED_OXYGEN, new AverageCount(4.0, 1L),
                DeviceType.DISSOLVED_TEMP, new AverageCount(5.0, 1L),
                DeviceType.PH, new AverageCount(6.0, 1L),
                DeviceType.DISSOLVED_EC, new AverageCount(7.0, 1L),
                DeviceType.DISSOLVED_TDS, new AverageCount(8.0, 1L)
        );
        when(sensorDataRepository.getLatestAverages(eq("sys"), eq("layer"), anyList()))
                .thenReturn(sensorMap);
        Map<DeviceType, AverageCount> actuatorMap = Map.of(
                DeviceType.AIR_PUMP, new AverageCount(1.0, 1L)
        );
        when(actuatorStatusRepository.getLatestActuatorAverages(eq("sys"), eq("layer"), anyList()))
                .thenReturn(actuatorMap);

        StatusAllAverageResponse response = statusService.getAllAverages("sys", "layer");

        assertEquals(1.0, response.growSensors().get("light").average());
        assertEquals(2.0, response.growSensors().get("humidity").average());
        assertEquals(3.0, response.growSensors().get("temperature").average());
        assertEquals(5.0, response.waterTank().get("dissolvedTemp").average());
        assertEquals(4.0, response.waterTank().get("dissolvedOxygen").average());
        assertEquals(6.0, response.waterTank().get("pH").average());
        assertEquals(7.0, response.waterTank().get("dissolvedEC").average());
        assertEquals(8.0, response.waterTank().get("dissolvedTDS").average());
        assertEquals(1.0, response.airpump().average());

        verify(sensorDataRepository).getLatestAverages(eq("sys"), eq("layer"), anyList());
        verify(actuatorStatusRepository).getLatestActuatorAverages(eq("sys"), eq("layer"), anyList());
    }
 /*
    @Test
    void getLiveNowSnapshotAggregatesByDevice() {
        java.time.Instant now = java.time.Instant.now();
        List<LiveNowRow> sensorRows = Arrays.asList(
                new LiveNowRow("S01", "L01", "light", "lux", 2.0, 2L, now),
                new LiveNowRow("S01", "L01", "humidity", "%", 3.0, 3L, now),
                new LiveNowRow("S01", "L01", "temperature", "°C", 4.0, 4L, now),
                new LiveNowRow("S01", "L01", "dissolvedTemp", "°C", 5.0, 5L, now),
                new LiveNowRow("S01", "L01", "dissolvedOxygen", "mg/L", 6.0, 6L, now),
                new LiveNowRow("S01", "L01", "pH", "pH", 7.0, 7L, now),
                new LiveNowRow("S01", "L01", "dissolvedEC", "mS/cm", 8.0, 8L, now),
                new LiveNowRow("S01", "L01", "dissolvedTDS", "ppm", 9.0, 9L, now),
                new LiveNowRow("S02", "L01", "dissolvedOxygen", "mg/L", 6.0, 6L, now)
        );
        List<LiveNowRow> actuatorRows = Arrays.asList(
                new LiveNowRow("S01", "L01", "airPump", "status", 1.0, 1L, now)
        );

        when(sensorDataRepository.fetchLatestSensorAverages(anyList())).thenReturn(sensorRows);
        when(actuatorStatusRepository.fetchLatestActuatorAverages(anyList())).thenReturn(actuatorRows);

        LiveNowSnapshot result = statusService.getLiveNowSnapshot();
        SystemSnapshot s01System = result.systems().get("S01");
        List<SystemSnapshot.LayerSnapshot> s01Layers = s01System.layers();
        assertEquals(1, s01Layers.size());
        SystemSnapshot.LayerSnapshot s01Layer = s01Layers.get(0);
        SystemSnapshot.LayerSnapshot s02Layer = result.systems().get("S02").layers().get(0);
        assertEquals(new StatusAverageResponse(1.0, "status",1L), s01Layer.actuators().airPump());
        assertEquals(new StatusAverageResponse(2.0, "lux",2L), s01Layer.environment().light());
        assertEquals(new StatusAverageResponse(6.0, "mg/L",6L), s02Layer.water().dissolvedOxygen());
        assertEquals(new StatusAverageResponse(1.0, "status",1L), s01System.actuators().airPump());
        assertEquals(new StatusAverageResponse(2.0, "lux",2L), s01System.environment().light());
        assertEquals(new StatusAverageResponse(6.0, "mg/L",6L), result.systems().get("S02").water().dissolvedOxygen());

        verify(sensorDataRepository).fetchLatestSensorAverages(anyList());
        verify(actuatorStatusRepository).fetchLatestActuatorAverages(anyList());
    }

    @Test
    void getLiveNowSnapshotAggregatesMultipleLayers() {
        java.time.Instant t1 = java.time.Instant.parse("2023-01-01T00:00:00Z");
        java.time.Instant t2 = java.time.Instant.parse("2023-01-02T00:00:00Z");

        List<LiveNowRow> sensorRows = Arrays.asList(
                new LiveNowRow("S01", "L01", "dissolvedTemp", "°C", 10.0, 1L, t1),
                new LiveNowRow("S01", "L02", "dissolvedTemp", "°C", 30.0, 1L, t2)
        );
        List<LiveNowRow> actuatorRows = Arrays.asList(
                new LiveNowRow("S01", "L01", "airPump", "status", 1.0, 1L, t1),
                new LiveNowRow("S01", "L02", "airPump", "status", 3.0, 1L, t2)
        );

        when(sensorDataRepository.fetchLatestSensorAverages(anyList())).thenReturn(sensorRows);
        when(actuatorStatusRepository.fetchLatestActuatorAverages(anyList())).thenReturn(actuatorRows);

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
        assertEquals(new StatusAverageResponse(1.0, "status",1L), layer1.actuators().airPump());
        assertEquals(new StatusAverageResponse(30.0, "°C",1L), layer2.water().dissolvedTemp());
    }

    @Test
    void getLiveNowSnapshotSkipsBlankSystemOrLayer() {
        java.time.Instant now = java.time.Instant.now();
        List<LiveNowRow> sensorRows = Arrays.asList(
                new LiveNowRow("", "L01", "light", "lux", 1.0, 1L, now),
                new LiveNowRow("S01", " ", "light", "lux", 1.0, 1L, now),
                new LiveNowRow(null, "L02", "light", "lux", 1.0, 1L, now),
                new LiveNowRow("S01", null, "light", "lux", 1.0, 1L, now),
                new LiveNowRow("S01", "L01", "light", "lux", 1.0, 1L, now)
        );
        List<LiveNowRow> actuatorRows = Arrays.asList(
                new LiveNowRow("S01", "L01", "airPump", "status", 1.0, 1L, now)
        );

        when(sensorDataRepository.fetchLatestSensorAverages(anyList())).thenReturn(sensorRows);
        when(actuatorStatusRepository.fetchLatestActuatorAverages(anyList())).thenReturn(actuatorRows);

        LiveNowSnapshot result = statusService.getLiveNowSnapshot();

        assertEquals(1, result.systems().size());
        SystemSnapshot system = result.systems().get("S01");
        List<SystemSnapshot.LayerSnapshot> layers = system.layers();
        assertEquals(1, layers.size());
        SystemSnapshot.LayerSnapshot layerSnapshot = layers.get(0);
        assertEquals(new StatusAverageResponse(1.0, "status",1L), layerSnapshot.actuators().airPump());
        assertEquals(new StatusAverageResponse(1.0, "status",1L), system.actuators().airPump());
        verify(sensorDataRepository).fetchLatestSensorAverages(anyList());
        verify(actuatorStatusRepository).fetchLatestActuatorAverages(anyList());
    }

   @Test
    void getLiveNowSnapshotSkipsRowsWithNullAvgOrDeviceCount() {
        java.time.Instant now = java.time.Instant.now();
        List<LiveNowRow> sensorRows = Arrays.asList(
                new LiveNowRow("S01", "L01", "light", "lux", null, 1L, now),
                new LiveNowRow("S01", "L01", "humidity", "%", 2.0, null, now),
                new LiveNowRow("S01", "L01", "temperature", "°C", 3.0, 3L, now)
        );
        when(sensorDataRepository.fetchLatestSensorAverages(anyList())).thenReturn(sensorRows);
        when(actuatorStatusRepository.fetchLatestActuatorAverages(anyList())).thenReturn(List.of());

        LiveNowSnapshot snapshot = statusService.getLiveNowSnapshot();

        SystemSnapshot system = snapshot.systems().get("S01");
        SystemSnapshot.LayerSnapshot layer = system.layers().get(0);

        assertEquals(new StatusAverageResponse(3.0, "°C",3L), layer.environment().temperature());
        assertEquals(null, layer.environment().light());
        assertEquals(null, layer.environment().humidity());
        assertEquals(new StatusAverageResponse(3.0, "°C",3L), system.environment().temperature());
        assertEquals(null, system.environment().light());
        assertEquals(null, system.environment().humidity());
    }*/

}
