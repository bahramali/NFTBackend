package se.hydroleaf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import se.hydroleaf.model.Device;
import se.hydroleaf.model.DeviceGroup;
import se.hydroleaf.model.SensorData;
import se.hydroleaf.model.SensorRecord;
import se.hydroleaf.repository.DeviceGroupRepository;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.SensorRecordRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RecordService focused on "spectral" style payloads.
 * Model: Option-1 (Device PK = composite_id). Comments are in English only.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecordServiceSpectralTests {

    @Autowired
    ObjectMapper objectMapper;
    @Autowired RecordService recordService;
    @Autowired DeviceRepository deviceRepository;
    @Autowired SensorRecordRepository recordRepository;
    @Autowired
    DeviceGroupRepository deviceGroupRepository;

    private DeviceGroup ensureGroup() {
        return deviceGroupRepository.findByMqttTopic("test-group").orElseGet(() -> {
            DeviceGroup g = new DeviceGroup();
            g.setMqttTopic("test-group");   // هر فیلدی که در entity‌ات ضروری است را ست کن
            return deviceGroupRepository.save(g);
        });
    }

    private Device ensureDevice(String compositeId) {
        return deviceRepository.findById(compositeId).orElseGet(() -> {
            Device d = new Device();
            d.setCompositeId(compositeId);
            // Optional denormalized fields if your entity still has them
            String[] parts = compositeId.split("-");
            if (parts.length >= 2) {
                d.setSystem(parts[0]);
                d.setLayer(parts[1]);
            }
            d.setGroup(ensureGroup());
            return deviceRepository.save(d);
        });
    }

    @Test
    void saves_record_with_empty_values_and_no_sensor_data() throws Exception {
        String compositeId = "S01-L01-esp32-01";
        ensureDevice(compositeId);

        String json = "{\n"
                + "  \"timestamp\": \"2024-01-01T00:00:00Z\",\n"
                + "  \"values\": {},\n"
                + "  \"health\": {}\n"
                + "}";

        JsonNode node = objectMapper.readTree(json);
        recordService.saveRecord(compositeId, node);

        List<SensorRecord> records = recordRepository.findAll();
        assertEquals(1, records.size(), "one record should be stored");
        assertTrue(records.get(0).getValues().isEmpty(), "no SensorData expected");
    }

    @Test
    void saves_named_spectral_channels_as_separate_sensor_data() throws Exception {
        String compositeId = "S01-L01-esp32-01";
        ensureDevice(compositeId);

        // Represent spectral channels as independent numeric sensors
        String json = "{\n"
                + "  \"timestamp\": \"2024-01-01T00:00:01Z\",\n"
                + "  \"values\": {\n"
                + "    \"spectrum_450\": {\"value\": 12.3, \"unit\": \"a.u.\"},\n"
                + "    \"spectrum_550\": {\"value\": 23.1, \"unit\": \"a.u.\"},\n"
                + "    \"spectrum_650\": {\"value\": 17.8, \"unit\": \"a.u.\"}\n"
                + "  }\n"
                + "}";

        JsonNode node = objectMapper.readTree(json);
        recordService.saveRecord(compositeId, node);

        List<SensorRecord> records = recordRepository.findAll();
        assertEquals(1, records.size(), "one record should be stored");
        List<SensorData> values = records.get(0).getValues();
        assertEquals(3, values.size(), "three spectral channels expected");

        // quick sanity on names and values
        assertTrue(values.stream().anyMatch(v -> "spectrum_450".equals(v.getSensorName()) && Double.valueOf(12.3).equals(v.getValue())));
        assertTrue(values.stream().anyMatch(v -> "spectrum_550".equals(v.getSensorName()) && Double.valueOf(23.1).equals(v.getValue())));
        assertTrue(values.stream().anyMatch(v -> "spectrum_650".equals(v.getSensorName()) && Double.valueOf(17.8).equals(v.getValue())));
    }

    @Test
    void saves_standard_env_values_alongside_spectral_channels() throws Exception {
        String compositeId = "S01-L01-esp32-02";
        ensureDevice(compositeId);

        String json = "{\n"
                + "  \"timestamp\": \"2024-01-01T00:00:02Z\",\n"
                + "  \"values\": {\n"
                + "    \"light\":       {\"value\": 55.4, \"unit\": \"lx\"},\n"
                + "    \"temperature\": {\"value\": 23.4, \"unit\": \"°C\"},\n"
                + "    \"humidity\":    {\"value\": 54.2, \"unit\": \"%\"},\n"
                + "    \"spectrum_500\": {\"value\": 9.9, \"unit\": \"a.u.\"}\n"
                + "  }\n"
                + "}";

        JsonNode node = objectMapper.readTree(json);
        recordService.saveRecord(compositeId, node);

        List<SensorRecord> records = recordRepository.findAll();
        assertEquals(1, records.size());
        List<SensorData> values = records.get(0).getValues();
        assertEquals(4, values.size());
        assertTrue(values.stream().anyMatch(v -> "light".equals(v.getSensorName())));
        assertTrue(values.stream().anyMatch(v -> "temperature".equals(v.getSensorName())));
        assertTrue(values.stream().anyMatch(v -> "humidity".equals(v.getSensorName())));
        assertTrue(values.stream().anyMatch(v -> "spectrum_500".equals(v.getSensorName())));
    }
}
