package se.hydroleaf.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import se.hydroleaf.repository.dto.history.AggregatedHistoryResponse;
import se.hydroleaf.repository.dto.history.AggregatedSensorData;
import se.hydroleaf.repository.dto.history.TimestampValue;
import se.hydroleaf.service.RecordService;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class RecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecordService recordService;

    @Test
    void aggregatedHistoryReturnsTemperatureData() throws Exception {
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
                        .param("bucket", "5m"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensors[0].sensorType").value("temperature"))
                .andExpect(jsonPath("$.sensors[0].data[0].value").value(25.0));
    }

    @Test
    void aggregatedHistoryReturnsMultipleSensors() throws Exception {
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
                        .param("sensorType", "humidity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensors.length()" ).value(2));
    }
}
