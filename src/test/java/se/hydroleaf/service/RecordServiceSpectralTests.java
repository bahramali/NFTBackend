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
    void spectralDataIsSplitIntoFourSensors() {
        String json = """
            {
              \"deviceId\": \"dev1\",
              \"location\": \"loc\",
              \"timestamp\": \"2024-01-01T00:00:00Z\",
              \"sensors\": [
                {
                  \"sensorId\": \"spec1\",
                  \"type\": \"veml7700\",
                  \"unit\": \"lux\",
                  \"value\": {
                    \"415nm\":1,\"445nm\":2,\"480nm\":3,\"515nm\":4,
                    \"555nm\":5,\"590nm\":6,\"630nm\":7,\"680nm\":8,
                    \"clear\":9,\"nir\":10
                  }
                }
              ]
            }
            """;

        recordService.saveMessage("growSensors", json);

        List<SensorRecord> records = recordRepository.findAll();
        assertEquals(1, records.size());
        List<SensorData> sensors = records.get(0).getSensors();
        assertEquals(4, sensors.size());
        Set<String> types = sensors.stream().map(SensorData::getType).collect(Collectors.toSet());
        assertTrue(types.contains("blueLight"));
        assertTrue(types.contains("redLight"));
        assertTrue(types.contains("clear"));
        assertTrue(types.contains("nir"));
    }
}
