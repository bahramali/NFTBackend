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
import se.hydroleaf.repository.dto.history.AggregatedHistoryResponse;
import se.hydroleaf.repository.dto.history.AggregatedSensorData;
import se.hydroleaf.repository.dto.history.TimestampValue;
import se.hydroleaf.service.AuthenticatedUser;
import se.hydroleaf.service.AuthorizationService;
import se.hydroleaf.service.JwtService;
import se.hydroleaf.service.RecordService;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecordService recordService;

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
    void aggregatedHistoryReturnsTemperatureData() throws Exception {
        when(authorizationService.requireMonitoringView(anyString())).thenReturn(adminUser());
        AggregatedSensorData tempData = new AggregatedSensorData(
                "temperature",
                "°C",
                List.of(new TimestampValue(Instant.parse("2023-01-01T00:00:00Z"), 25.0))
        );
        AggregatedHistoryResponse response = new AggregatedHistoryResponse(
                Instant.parse("2023-01-01T00:00:00Z"),
                Instant.parse("2023-01-02T00:00:00Z"),
                List.of(tempData)
        );
        when(recordService.aggregatedHistory(eq("dev1"), any(), any(), eq("5m"), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(response);

        mockMvc.perform(get("/api/records/history/aggregated")
                        .param("compositeId", "dev1")
                        .param("from", "2023-01-01T00:00:00Z")
                        .param("to", "2023-01-02T00:00:00Z")
                        .param("bucket", "5m")
                        .header("Authorization", "Bearer admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensors[0].sensorType").value("temperature"))
                .andExpect(jsonPath("$.sensors[0].data[0].value").value(25.0));
    }

    @Test
    void aggregatedHistoryReturnsMultipleSensors() throws Exception {
        when(authorizationService.requireMonitoringView(anyString())).thenReturn(adminUser());
        AggregatedSensorData tempData = new AggregatedSensorData(
                "temperature",
                "°C",
                List.of(new TimestampValue(Instant.parse("2023-01-01T00:00:00Z"), 25.0))
        );
        AggregatedSensorData humidityData = new AggregatedSensorData(
                "humidity",
                "%",
                List.of(new TimestampValue(Instant.parse("2023-01-01T00:00:00Z"), 60.0))
        );
        AggregatedHistoryResponse response = new AggregatedHistoryResponse(
                Instant.parse("2023-01-01T00:00:00Z"),
                Instant.parse("2023-01-02T00:00:00Z"),
                List.of(tempData, humidityData)
        );
        when(recordService.aggregatedHistory(eq("dev1"), any(), any(), eq("5m"),
                eq(List.of("temperature", "humidity")), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(response);

        mockMvc.perform(get("/api/records/history/aggregated")
                        .param("compositeId", "dev1")
                        .param("from", "2023-01-01T00:00:00Z")
                        .param("to", "2023-01-02T00:00:00Z")
                        .param("bucket", "5m")
                        .param("sensorType", "temperature")
                        .param("sensorType", "humidity")
                        .header("Authorization", "Bearer admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensors.length()" ).value(2));
    }

    @Test
    void aggregatedHistoryMapsNodeIdAndMetric() throws Exception {
        when(authorizationService.requireMonitoringView(anyString())).thenReturn(adminUser());
        AggregatedSensorData tempData = new AggregatedSensorData(
                "temp",
                "°C",
                List.of(new TimestampValue(Instant.parse("2023-01-01T00:00:00Z"), 21.0))
        );
        AggregatedHistoryResponse response = new AggregatedHistoryResponse(
                Instant.parse("2023-01-01T00:00:00Z"),
                Instant.parse("2023-01-02T00:00:00Z"),
                List.of(tempData)
        );
        when(recordService.aggregatedHistory(eq("node-1"), any(), any(), eq("5m"),
                eq(List.of("temp")), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(response);

        mockMvc.perform(get("/api/records/history/aggregated")
                        .param("nodeId", "node-1")
                        .param("metric", "temp")
                        .param("from", "2023-01-01T00:00:00Z")
                        .param("to", "2023-01-02T00:00:00Z")
                        .param("bucket", "5m")
                        .header("Authorization", "Bearer admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensors[0].sensorType").value("temp"));
    }
}
