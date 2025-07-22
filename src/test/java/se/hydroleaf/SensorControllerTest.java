package se.hydroleaf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import se.hydroleaf.model.SensorRecord;
import se.hydroleaf.repository.SensorRecordRepository;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SensorControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SensorRecordRepository repository;

    @BeforeEach
    void setup() {
        repository.deleteAll();
        SensorRecord record = new SensorRecord();
        record.setDeviceId("dev1");
        record.setTimestamp(Instant.now());
        record.setLocation("loc");
        record.setSensors("{\"temperature\":20.5}");
        record.setHealth("{}");
        repository.save(record);
    }

    @Test
    void latestShouldReturnRecord() throws Exception {
        mvc.perform(get("/api/sensors/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("dev1"));
    }
}
