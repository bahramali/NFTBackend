package se.hydroleaf.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import se.hydroleaf.model.Permission;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.repository.dto.DeviceSensorsResponse;
import se.hydroleaf.service.AuthenticatedUser;
import se.hydroleaf.service.AuthorizationService;
import se.hydroleaf.service.DeviceService;
import se.hydroleaf.service.JwtService;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
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

    @MockitoBean
    private AuthorizationService authorizationService;

    @MockitoBean
    private JwtService jwtService;

    private AuthenticatedUser adminUser() {
        return new AuthenticatedUser(1L, UserRole.ADMIN, Set.<Permission>of());
    }

    @BeforeEach
    void setupAuth() {
        when(jwtService.parseAccessToken(anyString())).thenReturn(adminUser());
    }

    @Test
    void getSensorsForDevicesReturnsAggregate() throws Exception {
        when(authorizationService.requireAdminOrOperator(anyString())).thenReturn(adminUser());
        DeviceSensorsResponse response = new DeviceSensorsResponse(
                "2025-08-22T09:05Z",
                List.of(new DeviceSensorsResponse.SystemInfo("S01", List.of("L01"), List.of("S01-R01-L01-G01"))),
                List.of(new DeviceSensorsResponse.DeviceInfo("S01", "R01", "L01", "G01", List.of("ph", "tds")))
        );
        when(deviceService.getSensorsForDevices(List.of("S01-R01-L01-G01"))).thenReturn(response);

        mockMvc.perform(get("/api/devices/sensors")
                        .header("Authorization", "Bearer admin")
                        .param("compositeIds", "S01-R01-L01-G01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.devices[0].systemId").value("S01"))
                .andExpect(jsonPath("$.devices[0].sensors[0]").value("ph"));
    }

    @Test
    void getSensorsForDevicesUnknownDeviceReturnsBadRequest() throws Exception {
        when(authorizationService.requireAdminOrOperator(anyString())).thenReturn(adminUser());
        when(deviceService.getSensorsForDevices(List.of("unknown")))
                .thenThrow(new IllegalArgumentException("Unknown device"));

        mockMvc.perform(get("/api/devices/sensors")
                        .header("Authorization", "Bearer admin")
                        .param("compositeIds", "unknown"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllDevicesWithSensorsReturnsAggregate() throws Exception {
        when(authorizationService.requireAdminOrOperator(anyString())).thenReturn(adminUser());
        DeviceSensorsResponse response = new DeviceSensorsResponse(
                "2025-08-22T09:05Z",
                List.of(new DeviceSensorsResponse.SystemInfo("S01", List.of("L01"), List.of("S01-R01-L01-G01"))),
                List.of(new DeviceSensorsResponse.DeviceInfo("S01", "R01", "L01", "G01", List.of("ph")))
        );
        when(deviceService.getAllDevicesWithSensors()).thenReturn(response);

        mockMvc.perform(get("/api/devices/all").header("Authorization", "Bearer admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.devices[0].deviceId").value("G01"))
                .andExpect(jsonPath("$.systems[0].compositeIds[0]").value("S01-R01-L01-G01"));
    }

    @Test
    void getCompositeIdsReturnsIds() throws Exception {
        when(authorizationService.requireAdminOrOperator(anyString())).thenReturn(adminUser());
        when(deviceService.getCompositeIds("S01", "R01", "L01", "D01"))
                .thenReturn(List.of("S01-R01-L01-D01"));

        mockMvc.perform(get("/api/devices/composite-ids")
                        .header("Authorization", "Bearer admin")
                        .param("system", "S01")
                        .param("rack", "R01")
                        .param("layer", "L01")
                        .param("deviceId", "D01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("S01-R01-L01-D01"));
    }
}
