package se.hydroleaf.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import se.hydroleaf.repository.dto.DeviceSensorsResponse;
import se.hydroleaf.service.DeviceService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeviceService deviceService;

    @Test
    void getSensorsForDevicesReturnsAggregate() throws Exception {
        DeviceSensorsResponse response = new DeviceSensorsResponse(
                "2025-08-22T09:05Z",
                List.of(new DeviceSensorsResponse.SystemInfo("S01", List.of("L01"))),
                List.of(new DeviceSensorsResponse.DeviceInfo("S01", "L01", "G01", List.of("ph", "tds")))
        );
        when(deviceService.getSensorsForDevices(List.of("S01:L01:G01"))).thenReturn(response);

        mockMvc.perform(get("/api/devices/sensors").param("compositeIds", "S01:L01:G01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.devices[0].systemId").value("S01"))
                .andExpect(jsonPath("$.devices[0].sensors[0]").value("ph"));
    }

    @Test
    void getSensorsForDevicesUnknownDeviceReturnsBadRequest() throws Exception {
        when(deviceService.getSensorsForDevices(List.of("unknown")))
                .thenThrow(new IllegalArgumentException("Unknown device"));

        mockMvc.perform(get("/api/devices/sensors").param("compositeIds", "unknown"))
                .andExpect(status().isBadRequest());
    }
}
