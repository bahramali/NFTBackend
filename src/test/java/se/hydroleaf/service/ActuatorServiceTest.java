package se.hydroleaf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.hydroleaf.model.OxygenPumpStatus;
import se.hydroleaf.repository.OxygenPumpStatusRepository;
import se.hydroleaf.util.InstantUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActuatorServiceTest {

    @Mock
    private OxygenPumpStatusRepository oxygenPumpStatusRepository;

    private ActuatorService actuatorService;

    @BeforeEach
    void setUp() {
        actuatorService = new ActuatorService(oxygenPumpStatusRepository, new ObjectMapper());
    }

    @Test
    void updatesExistingStatusWhenPresent() {
        OxygenPumpStatus existing = new OxygenPumpStatus();
        existing.setId(1L);
        when(oxygenPumpStatusRepository.findTopByOrderByIdAsc()).thenReturn(Optional.of(existing));

        String json = "{\"timestamp\":\"2023-01-01T00:00:00Z\",\"status\":true}";
        actuatorService.saveOxygenPumpStatus(json);

        ArgumentCaptor<OxygenPumpStatus> captor = ArgumentCaptor.forClass(OxygenPumpStatus.class);
        verify(oxygenPumpStatusRepository).save(captor.capture());
        OxygenPumpStatus saved = captor.getValue();
        assertSame(existing, saved);
        assertEquals(InstantUtil.parse("2023-01-01T00:00:00Z"), saved.getTimestamp());
        assertTrue(saved.getStatus());
    }

    @Test
    void createsNewStatusWhenNoneExists() {
        when(oxygenPumpStatusRepository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());

        String json = "{\"timestamp\":\"2023-01-01T00:00:00Z\",\"status\":false}";
        actuatorService.saveOxygenPumpStatus(json);

        ArgumentCaptor<OxygenPumpStatus> captor = ArgumentCaptor.forClass(OxygenPumpStatus.class);
        verify(oxygenPumpStatusRepository).save(captor.capture());
        OxygenPumpStatus saved = captor.getValue();
        assertNull(saved.getId());
        assertEquals(InstantUtil.parse("2023-01-01T00:00:00Z"), saved.getTimestamp());
        assertFalse(saved.getStatus());
    }

    @Test
    void parsesNumericStatusValues() {
        when(oxygenPumpStatusRepository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());

        String json = "{\"timestamp\":\"2023-01-01T00:00:00Z\",\"status\":\"1\"}";
        actuatorService.saveOxygenPumpStatus(json);

        ArgumentCaptor<OxygenPumpStatus> captor = ArgumentCaptor.forClass(OxygenPumpStatus.class);
        verify(oxygenPumpStatusRepository).save(captor.capture());
        OxygenPumpStatus saved = captor.getValue();
        assertNull(saved.getId());
        assertEquals(InstantUtil.parse("2023-01-01T00:00:00Z"), saved.getTimestamp());
        assertTrue(saved.getStatus());
    }
}
