package se.hydroleaf.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional
    void spectrumValuesAreStoredAsSeparateRows() {
        String json = """
            {
              "deviceId": "esp-1",
              "timestamp": "2024-01-01T00:00:00Z",
              "location": "test",
              "sensors": [
                {
                  "sensorName": "spec1",
                  "valueType": "445nm",
                  "unit": "count",
                  "value": 10
                },
                {
                  "sensorName": "spec1",
                  "valueType": "480nm",
                  "unit": "count",
                  "value": 20
                }
              ]
            }
            """;

        recordService.saveMessage("growSensors", json);

        List<SensorRecord> records = recordRepository.findAll();
        assertEquals(1, records.size());
        List<SensorData> sensors = records.get(0).getSensors();
        assertEquals(2, sensors.size());

        Set<String> types = sensors.stream().map(SensorData::getValueType).collect(Collectors.toSet());
        assertTrue(types.contains("445nm"));
        assertTrue(types.contains("480nm"));

        for (SensorData sd : sensors) {
            if ("445nm".equals(sd.getValueType())) {
                assertEquals(10.0, sd.getValue());
            }
            if ("480nm".equals(sd.getValueType())) {
                assertEquals(20.0, sd.getValue());
            }
        }
    }

}
