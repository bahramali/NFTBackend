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
        when(statusService.getAverage("sys", "layer", "lux"))
                .thenReturn(new StatusAverageResponse(12.5, 3L));

        mockMvc.perform(get("/api/status/sys/layer/lux/average"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.average").value(12.5))
                .andExpect(jsonPath("$.deviceCount").value(3));
    }

    @Test
    void getAverageEndpointAcceptsDifferentCase() throws Exception {
        when(statusService.getAverage("SYS", "LAYER", "Lux"))
                .thenReturn(new StatusAverageResponse(8.0, 1L));
        mockMvc.perform(get("/api/status/SYS/LAYER/Lux/average"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.average").value(8.0))
                .andExpect(jsonPath("$.deviceCount").value(1));
    }

    @Test
    void getAllAveragesEndpointReturnsData() throws Exception {
        StatusAllAverageResponse response = new StatusAllAverageResponse(
                new StatusAverageResponse(1.0,1L),
                new StatusAverageResponse(2.0,2L),
                new StatusAverageResponse(3.0,3L),
                new StatusAverageResponse(4.0,4L),
                new StatusAverageResponse(5.0,5L)
        );
        when(statusService.getAllAverages("sys", "layer")).thenReturn(response);

        mockMvc.perform(get("/api/status/sys/layer/all/average"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.light.average").value(1.0))
                .andExpect(jsonPath("$.humidity.average").value(2.0))
                .andExpect(jsonPath("$.temperature.average").value(3.0))
                .andExpect(jsonPath("$.dissolvedOxygen.average").value(4.0))
                .andExpect(jsonPath("$.airpump.average").value(5.0));
    }
}
