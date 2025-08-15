package se.hydroleaf.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import se.hydroleaf.dto.StatusAllAverageResponse;
import se.hydroleaf.dto.StatusAverageResponse;
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
    void getAverageEndpointReturnsWaterTankSensorData() throws Exception {
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
                "airTemperature", new StatusAverageResponse(3.0, "°C",3L)
        );
        Map<String, StatusAverageResponse> waterTank = Map.of(
                "waterTemperature", new StatusAverageResponse(4.0, "°C",4L),
                "dissolvedOxygen", new StatusAverageResponse(5.0, "mg/L",5L),
                "pH", new StatusAverageResponse(6.0, "pH",6L),
                "electricalConductivity", new StatusAverageResponse(7.0, "µS/cm",7L)
        );
        StatusAllAverageResponse response = new StatusAllAverageResponse(
                growSensors,
                waterTank,
                new StatusAverageResponse(8.0, "status",8L)
        );
        when(statusService.getAllAverages("sys", "layer")).thenReturn(response);

        mockMvc.perform(get("/api/status/sys/layer/all/average"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.growSensors.light.average").value(1.0))
                .andExpect(jsonPath("$.growSensors.humidity.average").value(2.0))
                .andExpect(jsonPath("$.growSensors.airTemperature.average").value(3.0))
                .andExpect(jsonPath("$.waterTank.waterTemperature.average").value(4.0))
                .andExpect(jsonPath("$.waterTank.dissolvedOxygen.average").value(5.0))
                .andExpect(jsonPath("$.waterTank.pH.average").value(6.0))
                .andExpect(jsonPath("$.waterTank.electricalConductivity.average").value(7.0))
                .andExpect(jsonPath("$.airpump.average").value(8.0));
    }
}
