package se.hydroleaf.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import se.hydroleaf.model.Device;
import se.hydroleaf.model.GerminationCycle;
import se.hydroleaf.model.TopicName;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.GerminationCycleRepository;
import se.hydroleaf.service.GerminationService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GerminationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private GerminationCycleRepository germinationCycleRepository;

    @MockBean
    private Clock clock;

    @Autowired
    private GerminationService germinationService;

    private Device device;

    @BeforeEach
    void setup() {
        // Clean DB tables to avoid unique violations between tests
        germinationCycleRepository.deleteAllInBatch();
        deviceRepository.deleteAllInBatch();

        device = Device.builder()
                .compositeId("S01-L02-G03")
                .system("S01")
                .layer("L02")
                .deviceId("G03")
                .topic(TopicName.germinationTopic)
                .build();
        device = deviceRepository.saveAndFlush(device);

        Mockito.reset(clock);
        Mockito.when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        Mockito.when(clock.instant()).thenReturn(Instant.EPOCH);
    }

    @Test
    void triggerStartCreatesOrUpdatesCycle() throws Exception {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant later = start.plusSeconds(60);
        Mockito.when(clock.instant()).thenReturn(start, later);

        mockMvc.perform(post("/api/germination/{system}/{layer}/{deviceId}/start", "S01", "L02", "G03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compositeId").value("S01-L02-G03"))
                .andExpect(jsonPath("$.startTime").value(start.toString()))
                .andExpect(jsonPath("$.elapsedSeconds").value(60));

        assertThat(germinationCycleRepository.findById("S01-L02-G03"))
                .isPresent()
                .get()
                .extracting(GerminationCycle::getStartTime)
                .isEqualTo(start);
    }

    @Test
    void updateStartManuallySetsTimestamp() throws Exception {
        germinationService.updateStart(device.getCompositeId(), Instant.parse("2023-01-01T00:00:00Z"));

        Instant newStart = Instant.parse("2024-05-10T12:30:00Z");
        Mockito.when(clock.instant()).thenReturn(newStart.plusSeconds(120));

        String payload = "{\"startTime\":\"" + newStart + "\"}";

        mockMvc.perform(put("/api/germination/{system}/{layer}/{deviceId}", "S01", "L02", "G03")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime").value(newStart.toString()))
                .andExpect(jsonPath("$.elapsedSeconds").value(120));

        assertThat(germinationCycleRepository.findById("S01-L02-G03"))
                .isPresent()
                .get()
                .extracting(GerminationCycle::getStartTime)
                .isEqualTo(newStart);
    }

    @Test
    void getStatusReturnsElapsedTime() throws Exception {
        Instant start = Instant.parse("2024-02-01T06:00:00Z");
        germinationService.updateStart(device.getCompositeId(), start);

        Mockito.when(clock.instant()).thenReturn(start.plusSeconds(3600));

        mockMvc.perform(get("/api/germination/{system}/{layer}/{deviceId}", "S01", "L02", "G03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime").value(start.toString()))
                .andExpect(jsonPath("$.elapsedSeconds").value(3600));
    }

    @Test
    void getStatusReturns404WhenMissing() throws Exception {
        mockMvc.perform(get("/api/germination/{system}/{layer}/{deviceId}", "S01", "L02", "G03"))
                .andExpect(status().isNotFound());
    }
}

