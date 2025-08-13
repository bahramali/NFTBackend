package se.hydroleaf.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import se.hydroleaf.model.Device;
import se.hydroleaf.repository.DeviceRepository;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RecordServiceDeviceTests {

    @Autowired
    private RecordService recordService;

    @Autowired
    private DeviceRepository deviceRepository;

    @Test
    @Transactional
    void deviceSystemAndIdentifiersAreStored() {
        String json = """
            {
              "system": "S02",
              "deviceId": "G01",
              "layer": "L02",
              "compositeId": "S02-L02-G01",
              "timestamp": "2025-08-13T21:13:31Z",
              "sensors": [
                {
                  "sensorName": "VEML7700",
                  "valueType": "light",
                  "value": 19.3536,
                  "unit": "lux"
                }
              ],
              "health": {
                "sht3x": false,
                "veml7700": true,
                "as7343": false
              }
            }
            """;

        recordService.saveMessage("growSensors", json);

        Device device = deviceRepository.findById("G01").orElseThrow();
        assertEquals("S02", device.getSystem());
        assertEquals("L02", device.getLayer());
        assertEquals("S02-L02-G01", device.getCompositeId());
    }
}
