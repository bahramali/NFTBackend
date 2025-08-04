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
        String sensorName = "spec1";
        String valueType1 = "445nm";
        String valueType2 = "480nm";

        String json = """
            {
              "deviceId": "esp-1",
              "timestamp": "2024-01-01T00:00:00Z",
              "location": "test",
              "sensors": [
                {
                  "sensorName": "%s",
                  "valueType": "%s",
                  "unit": "count",
                  "value": 10
                },
                {
                  "sensorName": "%s",
                  "valueType": "%s",
                  "unit": "count",
                  "value": 20
                }
              ]
            }
            """.formatted(sensorName, valueType1, sensorName, valueType2);

        recordService.saveMessage("growSensors", json);

        List<SensorRecord> records = recordRepository.findAll();
        assertEquals(1, records.size());
        List<SensorData> sensors = records.get(0).getSensors();
        assertEquals(2, sensors.size());

        Set<String> types = sensors.stream().map(SensorData::getValueType).collect(Collectors.toSet());
        assertTrue(types.contains(valueType1));
        assertTrue(types.contains(valueType2));

        for (SensorData sd : sensors) {
            if (valueType1.equals(sd.getValueType())) {
                assertEquals(10.0, sd.getValue());
            }
            if (valueType2.equals(sd.getValueType())) {
                assertEquals(20.0, sd.getValue());
            }
        }
    }

    @Test
    @Transactional
    void waterTankMessageIsStored() {
        String sensorName = "tank1";
        String json = """
            {
              "deviceId": "tank-1",
              "timestamp": "2024-01-01T00:00:00Z",
              "location": "test",
              "sensors": [
                {
                  "sensorName": "%s",
                  "valueType": "level",
                  "unit": "percent",
                  "value": 70
                },
                {
                  "sensorName": "%s",
                  "valueType": "temperature",
                  "unit": "C",
                  "value": 22
                }
              ]
            }
            """.formatted(sensorName, sensorName);

        recordService.saveMessage("waterTank", json);

        List<SensorRecord> records = recordRepository.findAll();
        assertEquals(1, records.size());
        List<SensorData> sensors = records.get(0).getSensors();
        assertEquals(2, sensors.size());

        Set<String> types = sensors.stream().map(SensorData::getValueType).collect(Collectors.toSet());
        assertTrue(types.contains("level"));
        assertTrue(types.contains("temperature"));
    }

}
