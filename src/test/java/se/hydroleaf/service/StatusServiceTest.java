package se.hydroleaf.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Spy;
import se.hydroleaf.dto.snapshot.LiveNowSnapshot;
import se.hydroleaf.dto.snapshot.SystemSnapshot;
import se.hydroleaf.dto.summary.StatusAllAverageResponse;
import se.hydroleaf.dto.summary.StatusAverageResponse;
import se.hydroleaf.repository.ActuatorStatusRepository;
import se.hydroleaf.repository.AverageCount;
import se.hydroleaf.repository.SensorDataRepository;
import se.hydroleaf.repository.dto.LiveNowRow;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatusServiceTest {

    @Mock
    private SensorDataRepository sensorDataRepository;

    @Mock
    private ActuatorStatusRepository actuatorStatusRepository;

    @Spy
    @InjectMocks
    private StatusService statusService;

    private record Row(String system, String layer, String type, Double average, Long count) implements LiveNowRow {
        public String getSystem() { return system; }
        public String getLayer() { return layer; }
        public String getType() { return type; }
        public Double getAverage() { return average; }
        public Long getCount() { return count; }
    }

    @Test
    void getAverageUsesSensorDataRepository() {
        when(sensorDataRepository.getLatestAverage("Sys", "Layer", "light"))
                .thenReturn(new AverageCount(10.0, 1L));

        StatusAverageResponse response = statusService.getAverage("Sys", "Layer", "light");

        assertEquals(10.0, response.average());
        assertEquals("lux", response.unit());
        assertEquals(1L, response.deviceCount());
        verify(sensorDataRepository).getLatestAverage("Sys", "Layer", "light");
        verifyNoInteractions(actuatorStatusRepository);
    }

    @Test
    void getAverageUsesSensorDataRepositoryForWaterTankSensor() {
        when(sensorDataRepository.getLatestAverage("Sys", "Layer", "dissolvedOxygen"))
                .thenReturn(new AverageCount(9.9, 1L));

        StatusAverageResponse response = statusService.getAverage("Sys", "Layer", "dissolvedOxygen");

        assertEquals(9.9, response.average());
        assertEquals("mg/L", response.unit());
        assertEquals(1L, response.deviceCount());
        verify(sensorDataRepository).getLatestAverage("Sys", "Layer", "dissolvedOxygen");
        verifyNoInteractions(actuatorStatusRepository);
    }

    @Test
    void getAverageUsesActuatorStatusRepositoryForAirpump() {
        when(actuatorStatusRepository.getLatestActuatorAverage("Sys", "Layer", "airpump"))
                .thenReturn(new AverageCount(1.0, 1L));

        StatusAverageResponse response = statusService.getAverage("Sys", "Layer", "airpump");

        assertEquals(1.0, response.average());
        assertEquals("status", response.unit());
        assertEquals(1L, response.deviceCount());
        verify(actuatorStatusRepository).getLatestActuatorAverage("Sys", "Layer", "airpump");
        verifyNoInteractions(sensorDataRepository);
    }

    @Test
    void getAllAveragesAggregatesAllSensorTypes() {
        when(sensorDataRepository.getLatestAverage("sys", "layer", "light"))
                .thenReturn(new AverageCount(1.0, 1L));
        when(sensorDataRepository.getLatestAverage("sys", "layer", "humidity"))
                .thenReturn(new AverageCount(2.0, 1L));
        when(sensorDataRepository.getLatestAverage("sys", "layer", "temperature"))
                .thenReturn(new AverageCount(3.0, 1L));
        when(sensorDataRepository.getLatestAverage("sys", "layer", "dissolvedOxygen"))
                .thenReturn(new AverageCount(4.0, 1L));
        when(sensorDataRepository.getLatestAverage("sys", "layer", "dissolvedTemp"))
                .thenReturn(new AverageCount(5.0, 1L));
        when(sensorDataRepository.getLatestAverage("sys", "layer", "pH"))
                .thenReturn(new AverageCount(6.0, 1L));
        when(sensorDataRepository.getLatestAverage("sys", "layer", "dissolvedEC"))
                .thenReturn(new AverageCount(7.0, 1L));
        when(sensorDataRepository.getLatestAverage("sys", "layer", "dissolvedTDS"))
                .thenReturn(new AverageCount(8.0, 1L));
        when(actuatorStatusRepository.getLatestActuatorAverage("sys", "layer", "airPump"))
                .thenReturn(new AverageCount(1.0, 1L));

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

        verify(sensorDataRepository).getLatestAverage("sys", "layer", "dissolvedTemp");
        verify(sensorDataRepository).getLatestAverage("sys", "layer", "pH");
        verify(sensorDataRepository).getLatestAverage("sys", "layer", "dissolvedEC");
        verify(sensorDataRepository).getLatestAverage("sys", "layer", "dissolvedTDS");
        verify(actuatorStatusRepository).getLatestActuatorAverage("sys", "layer", "airPump");
    }

    @Test
    void getLiveNowSnapshotAggregatesByDevice() {
        List<LiveNowRow> sensorRows = List.of(
                new Row("S01", "L01", "light", 2.0, 2L),
                new Row("S01", "L01", "humidity", 3.0, 3L),
                new Row("S01", "L01", "temperature", 4.0, 4L),
                new Row("S01", "L01", "dissolvedTemp", 5.0, 5L),
                new Row("S01", "L01", "dissolvedOxygen", 6.0, 6L),
                new Row("S01", "L01", "pH", 7.0, 7L),
                new Row("S01", "L01", "dissolvedEC", 8.0, 8L),
                new Row("S01", "L01", "dissolvedTDS", 9.0, 9L),
                new Row("S02", "L01", "dissolvedOxygen", 6.0, 6L)
        );
        List<LiveNowRow> actuatorRows = List.of(
                new Row("S01", "L01", "airPump", 1.0, 1L)
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
        List<LiveNowRow> sensorRows = List.of(
                new Row("S01", "L01", "dissolvedTemp", 10.0, 1L),
                new Row("S01", "L02", "dissolvedTemp", 30.0, 1L)
        );
        List<LiveNowRow> actuatorRows = List.of(
                new Row("S01", "L01", "airPump", 1.0, 1L),
                new Row("S01", "L02", "airPump", 3.0, 1L)
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
        List<LiveNowRow> sensorRows = List.of(
                new Row("", "L01", "light", 1.0, 1L),
                new Row("S01", " ", "light", 1.0, 1L),
                new Row(null, "L02", "light", 1.0, 1L),
                new Row("S01", null, "light", 1.0, 1L),
                new Row("S01", "L01", "light", 1.0, 1L)
        );
        List<LiveNowRow> actuatorRows = List.of(
                new Row("S01", "L01", "airPump", 1.0, 1L)
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

}
