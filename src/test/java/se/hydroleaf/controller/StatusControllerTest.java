package se.hydroleaf.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import se.hydroleaf.dto.snapshot.LiveNowSnapshot;
import se.hydroleaf.dto.snapshot.SystemSnapshot;
import se.hydroleaf.dto.summary.ActuatorStatusSummary;
import se.hydroleaf.dto.summary.GrowSensorSummary;
import se.hydroleaf.dto.summary.StatusAllAverageResponse;
import se.hydroleaf.dto.summary.StatusAverageResponse;
import se.hydroleaf.dto.summary.WaterTankSummary;
import se.hydroleaf.service.StatusService;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatusService statusService;

    @Test
    void getAverageEndpointReturnsData() throws Exception {
        when(statusService.getAverage("sys", "layer", "light"))
                .thenReturn(new StatusAverageResponse(12.5, "lux", 3L));

        mockMvc.perform(get("/api/status/sys/layer/light/average"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.average").value(12.5))
                .andExpect(jsonPath("$.unit").value("lux"))
                .andExpect(jsonPath("$.deviceCount").value(3));
    }

    @Test
    void getAverageEndpointReturnsActuatorData() throws Exception {
        when(statusService.getAverage("sys", "layer", "airPump"))
                .thenReturn(new StatusAverageResponse(1.0, "status", 2L));

        mockMvc.perform(get("/api/status/sys/layer/airPump/average"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.average").value(1.0))
                .andExpect(jsonPath("$.deviceCount").value(2));
    }

    @Test
    void getAverageEndpointReturnsWaterTankSensorValueData() throws Exception {
        when(statusService.getAverage("sys", "layer", "dissolvedOxygen"))
                .thenReturn(new StatusAverageResponse(5.5, "mg/L", 4L));

        mockMvc.perform(get("/api/status/sys/layer/dissolvedOxygen/average"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.average").value(5.5))
                .andExpect(jsonPath("$.deviceCount").value(4));
    }

    @Test
    void getAverageEndpointAcceptsDifferentCase() throws Exception {
        when(statusService.getAverage("SYS", "LAYER", "Light"))
                .thenReturn(new StatusAverageResponse(8.0, "lux", 1L));
        mockMvc.perform(get("/api/status/SYS/LAYER/Light/average"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.average").value(8.0))
                .andExpect(jsonPath("$.unit").value("lux"))
                .andExpect(jsonPath("$.deviceCount").value(1));
    }

    @Test
    void getAllAveragesEndpointReturnsData() throws Exception {
        Map<String, StatusAverageResponse> growSensors = Map.of(
                "light", new StatusAverageResponse(1.0, "lux",1L),
                "humidity", new StatusAverageResponse(2.0, "%",2L),
                "temperature", new StatusAverageResponse(3.0, "°C",3L)
        );
        Map<String, StatusAverageResponse> waterTank = Map.of(
                "dissolvedTemp", new StatusAverageResponse(4.0, "°C",4L),
                "dissolvedOxygen", new StatusAverageResponse(5.0, "mg/L",5L),
                "pH", new StatusAverageResponse(6.0, "pH",6L),
                "dissolvedEC", new StatusAverageResponse(7.0, "mS/cm",7L),
                "dissolvedTDS", new StatusAverageResponse(8.0, "ppm",8L)
        );
        StatusAllAverageResponse response = new StatusAllAverageResponse(
                growSensors,
                waterTank,
                new StatusAverageResponse(9.0, "status",9L)
        );
        when(statusService.getAllAverages("sys", "layer")).thenReturn(response);

        mockMvc.perform(get("/api/status/sys/layer/all/average"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.growSensors.light.average").value(1.0))
                .andExpect(jsonPath("$.growSensors.humidity.average").value(2.0))
                .andExpect(jsonPath("$.growSensors.temperature.average").value(3.0))
                .andExpect(jsonPath("$.waterTank.dissolvedTemp.average").value(4.0))
                .andExpect(jsonPath("$.waterTank.dissolvedOxygen.average").value(5.0))
                .andExpect(jsonPath("$.waterTank.pH.average").value(6.0))
                .andExpect(jsonPath("$.waterTank.dissolvedEC.average").value(7.0))
                .andExpect(jsonPath("$.waterTank.dissolvedTDS.average").value(8.0))
                .andExpect(jsonPath("$.airpump.average").value(9.0));
    }

    @Test
    void getLiveNowEndpointReturnsSystemData() throws Exception {
        SystemSnapshot.LayerSnapshot layer = new SystemSnapshot.LayerSnapshot(
                "L1",
                java.time.Instant.parse("2023-01-01T00:00:00Z"),
                new ActuatorStatusSummary(new StatusAverageResponse(1.0, "status", 1L)),
                new WaterTankSummary(
                        new StatusAverageResponse(4.0, "°C",1L),
                        new StatusAverageResponse(5.0, "mg/L",1L),
                        new StatusAverageResponse(6.0, "pH",1L),
                        new StatusAverageResponse(7.0, "mS/cm",1L),
                        new StatusAverageResponse(8.0, "ppm",1L)
                ),
                new GrowSensorSummary(
                        new StatusAverageResponse(2.0, "lux",1L),
                        new StatusAverageResponse(3.0, "%",1L),
                        new StatusAverageResponse(8.0, "°C",1L)
                )
        );
        SystemSnapshot system = new SystemSnapshot(
                java.time.Instant.parse("2023-01-01T00:00:00Z"),
                new ActuatorStatusSummary(new StatusAverageResponse(1.0, "status",1L)),
                new WaterTankSummary(
                        new StatusAverageResponse(4.0, "°C",1L),
                        new StatusAverageResponse(5.0, "mg/L",1L),
                        new StatusAverageResponse(6.0, "pH",1L),
                        new StatusAverageResponse(7.0, "mS/cm",1L),
                        new StatusAverageResponse(8.0, "ppm",1L)
                ),
                new GrowSensorSummary(
                        new StatusAverageResponse(2.0, "lux",1L),
                        new StatusAverageResponse(3.0, "%",1L),
                        new StatusAverageResponse(8.0, "°C",1L)
                ),
                java.util.List.of(layer)
        );
        LiveNowSnapshot snapshot = new LiveNowSnapshot(Map.of("sys", system));
        when(statusService.getLiveNowSnapshot()).thenReturn(snapshot);

        mockMvc.perform(get("/api/status/live-now"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systems.sys.actuators.airPump.average").value(1.0))
                .andExpect(jsonPath("$.systems.sys.environment.temperature.average").value(8.0))
                .andExpect(jsonPath("$.systems.sys.layers[0].environment.temperature.average").value(8.0))
                .andExpect(jsonPath("$.systems.sys.layers[0].water.dissolvedOxygen.average").value(5.0));
    }
}
