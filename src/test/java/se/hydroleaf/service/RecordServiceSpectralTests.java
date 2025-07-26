package se.hydroleaf.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.hydroleaf.model.SensorData;
import se.hydroleaf.model.SensorRecord;
import se.hydroleaf.repository.SensorRecordRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RecordServiceSpectralTests {

    @Autowired
    private RecordService recordService;

    @Autowired
    private SensorRecordRepository recordRepository;


    @Test
    void spectrumValuesAreStoredAsSeparateRows() {
        String json = """
            {
              "deviceId": "esp-1",
              "timestamp": "2024-01-01T00:00:00Z",
              "location": "test",
              "sensors": [
                {
                  "sensorId": "spec1",
                  "type": "color",
                  "unit": "count",
                  "value": {"spectrum": {"445": 10, "480": 20}}
                }
              ]
            }
            """;

        recordService.saveMessage("growSensors", json);

        List<SensorRecord> records = recordRepository.findAll();
        assertEquals(1, records.size());
        List<SensorData> sensors = records.get(0).getSensors();
        assertEquals(2, sensors.size());

        Set<String> types = sensors.stream().map(SensorData::getType).collect(Collectors.toSet());
        assertTrue(types.contains("color_445nm"));
        assertTrue(types.contains("color_480nm"));

        for (SensorData sd : sensors) {
            if ("color_445nm".equals(sd.getType())) {
                assertEquals(10.0, sd.getNumericValue());
            }
            if ("color_480nm".equals(sd.getType())) {
                assertEquals(20.0, sd.getNumericValue());
            }
            assertNull(sd.getSensorValue());
        }
    }

}
