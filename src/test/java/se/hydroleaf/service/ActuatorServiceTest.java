package se.hydroleaf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.hydroleaf.model.ActuatorStatus;
import se.hydroleaf.model.Device;
import se.hydroleaf.repository.ActuatorStatusRepository;
import se.hydroleaf.repository.DeviceRepository;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActuatorServiceTest {

    @Mock ActuatorStatusRepository actuatorRepo;
    @Mock DeviceRepository deviceRepo;

    private ActuatorService actuatorService;

    @BeforeEach
    void setup() {
        this.actuatorService = new ActuatorService(new ObjectMapper(), actuatorRepo, deviceRepo);
    }

    private Device device(String compositeId) {
        Device d = new Device();
        d.setCompositeId(compositeId);
        return d;
    }

    @Test
    void saves_status_when_payload_has_airPump_controller_with_string_state() {
        String compositeId = "S01-R01-L02-G01";
        when(deviceRepo.findById(compositeId)).thenReturn(Optional.of(device(compositeId)));

        String json = """
                {
                  "composite_id":"S01-R01-L02-G01",
                  "timestamp":"2023-01-01T00:00:00Z",
                  "sensors":[{"sensorName":"tempSensor","sensorType":"temperature","value":22.5}],
                  "health":{"tempSensor":true},
                  "controllers":[{"name":"airPump","state":"on"}]
                }
                """;

        actuatorService.saveActuatorStatus(json);

        ArgumentCaptor<ActuatorStatus> captor = ArgumentCaptor.forClass(ActuatorStatus.class);
        verify(actuatorRepo, times(1)).save(captor.capture());
        ActuatorStatus saved = captor.getValue();

        assertEquals(compositeId, saved.getDevice().getCompositeId());
        assertEquals(Instant.parse("2023-01-01T00:00:00Z"), saved.getTimestamp());
        assertTrue(saved.getState());
        assertEquals("airPump", saved.getActuatorType());
    }

    @Test
    void saves_status_when_payload_has_boolean_and_numeric_values() {
        String compositeId = "S02-R01-L01-X1";
        when(deviceRepo.findById(compositeId)).thenReturn(Optional.of(device(compositeId)));

        String jsonTrue = """
                {
                  "composite_id":"S02-R01-L01-X1",
                  "sensors":[{"sensorName":"s1","sensorType":"temperature","value":25.0}],
                  "health":{"s1":true},
                  "controllers":[{"name":"airPump","state":true,"timestamp":"2024-02-02T12:00:00Z"}]
                }
                """;
        String jsonNum  = """
                {
                  "composite_id":"S02-R01-L01-X1",
                  "sensors":[{"sensorName":"s1","sensorType":"temperature","value":25.0}],
                  "health":{"s1":true},
                  "controllers":[{"name":"airPump","state":1,"timestamp":"2024-02-02T12:01:00Z"}]
                }
                """;

        actuatorService.saveActuatorStatus(jsonTrue);
        actuatorService.saveActuatorStatus(jsonNum);

        ArgumentCaptor<ActuatorStatus> captor = ArgumentCaptor.forClass(ActuatorStatus.class);
        verify(actuatorRepo, times(2)).save(captor.capture());

        ActuatorStatus first = captor.getAllValues().get(0);
        ActuatorStatus second = captor.getAllValues().get(1);

        assertEquals(Instant.parse("2024-02-02T12:00:00Z"), first.getTimestamp());
        assertEquals(Instant.parse("2024-02-02T12:01:00Z"), second.getTimestamp());
        assertTrue(first.getState());
        assertTrue(second.getState());
        assertEquals(compositeId, first.getDevice().getCompositeId());
        assertEquals(compositeId, second.getDevice().getCompositeId());
        assertEquals("airPump", first.getActuatorType());
        assertEquals("airPump", second.getActuatorType());
    }

    @Test
    void throws_when_composite_id_missing() {
        String json = """
                {
                  "sensors":[{"sensorName":"t1","sensorType":"temperature","value":20.0}],
                  "health":{"t1":true},
                  "controllers":[{"name":"airPump","state":"off"}]
                }
                """;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> actuatorService.saveActuatorStatus(json));

        assertTrue(ex.getMessage().toLowerCase().contains("composite"));
        verifyNoInteractions(actuatorRepo);
}
}
