package se.hydroleaf.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.hydroleaf.model.Device;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.LatestSensorValueRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    DeviceRepository deviceRepository;

    @Mock
    LatestSensorValueRepository latestSensorValueRepository;

    private DeviceService deviceService;

    @BeforeEach
    void setup() {
        deviceService = new DeviceService(deviceRepository, latestSensorValueRepository);
    }

    @Test
    void getCompositeIdsFiltersByDeviceIdWhenProvided() {
        Device d1 = new Device();
        d1.setCompositeId("S01-L01-A1");
        d1.setDeviceId("A1");
        Device d2 = new Device();
        d2.setCompositeId("S01-L01-B1");
        d2.setDeviceId("B1");
        when(deviceRepository.findBySystemAndLayer("S01", "L01"))
                .thenReturn(List.of(d1, d2));

        List<String> result = deviceService.getCompositeIds("S01", "L01", "B1");

        assertEquals(List.of("S01-L01-B1"), result);
    }
}
