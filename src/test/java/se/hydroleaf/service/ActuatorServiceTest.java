package se.hydroleaf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.hydroleaf.model.Device;
import se.hydroleaf.model.OxygenPumpStatus;
import se.hydroleaf.repository.OxygenPumpStatusRepository;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.util.InstantUtil;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActuatorServiceTest {

    @Mock ObjectMapper objectMapper; // not strictly needed, but kept for parity
    @Mock OxygenPumpStatusRepository pumpRepo;
    @Mock DeviceRepository deviceRepo;

    private ActuatorService actuatorService;

    @BeforeEach
    void setup() {
        // Suppose your ActuatorService has this constructor; adjust if different.
        this.actuatorService = new ActuatorService(new ObjectMapper(), pumpRepo, deviceRepo);
    }

    private Device device(String compositeId) {
        Device d = new Device();
        d.setCompositeId(compositeId);
        return d;
    }

    @Test
    void saves_status_when_payload_has_composite_id_and_string_on_off() {
        String compositeId = "S01-L02-G01";
        when(deviceRepo.findById(compositeId)).thenReturn(Optional.of(device(compositeId)));

        String json = "{"
                + "\"composite_id\":\"S01-L02-G01\","
                + "\"timestamp\":\"2023-01-01T00:00:00Z\","
                + "\"status\":\"on\""
                + "}";

        actuatorService.saveOxygenPumpStatus(json);

        ArgumentCaptor<OxygenPumpStatus> captor = ArgumentCaptor.forClass(OxygenPumpStatus.class);
        verify(pumpRepo, times(1)).save(captor.capture());
        OxygenPumpStatus saved = captor.getValue();

        assertEquals(compositeId, saved.getDevice().getCompositeId());
        assertEquals(Instant.parse("2023-01-01T00:00:00Z"), saved.getTimestamp());
        assertTrue(saved.getStatus());
    }

    @Test
    void saves_status_when_payload_has_boolean_and_numeric_values() {
        String compositeId = "S02-L01-X1";
        when(deviceRepo.findById(compositeId)).thenReturn(Optional.of(device(compositeId)));

        String jsonTrue = "{\"composite_id\":\"S02-L01-X1\",\"timestamp\":\"2024-02-02T12:00:00Z\",\"status\":true}";
        String jsonNum  = "{\"composite_id\":\"S02-L01-X1\",\"timestamp\":\"2024-02-02T12:01:00Z\",\"status\":1}";

        actuatorService.saveOxygenPumpStatus(jsonTrue);
        actuatorService.saveOxygenPumpStatus(jsonNum);

        ArgumentCaptor<OxygenPumpStatus> captor = ArgumentCaptor.forClass(OxygenPumpStatus.class);
        verify(pumpRepo, times(2)).save(captor.capture());

        OxygenPumpStatus first = captor.getAllValues().get(0);
        OxygenPumpStatus second = captor.getAllValues().get(1);

        assertTrue(first.getStatus());
        assertTrue(second.getStatus());
        assertEquals(compositeId, first.getDevice().getCompositeId());
        assertEquals(compositeId, second.getDevice().getCompositeId());
    }

    @Test
    void throws_when_composite_id_missing() {
        String json = "{\"timestamp\":\"2024-01-01T00:00:00Z\",\"status\":\"off\"}";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> actuatorService.saveOxygenPumpStatus(json));

        assertTrue(ex.getMessage().toLowerCase().contains("composite"));
        verifyNoInteractions(pumpRepo);
    }
}
