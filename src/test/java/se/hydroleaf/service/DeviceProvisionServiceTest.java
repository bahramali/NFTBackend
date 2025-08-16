package se.hydroleaf.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.hydroleaf.model.Device;
import se.hydroleaf.model.DeviceGroup;
import se.hydroleaf.repository.DeviceGroupRepository;
import se.hydroleaf.repository.DeviceRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceProvisionServiceTest {

    @Mock
    private DeviceRepository deviceRepo;

    @Mock
    private DeviceGroupRepository groupRepo;

    @InjectMocks
    private DeviceProvisionService service;

    @Test
    void ensureDevicePreservesHyphenatedDeviceId() {
        String compositeId = "S01-L02-esp32-01";

        DeviceGroup group = new DeviceGroup();
        group.setMqttTopic("test");

        when(groupRepo.findByMqttTopic(anyString())).thenReturn(Optional.of(group));
        when(deviceRepo.findById(compositeId)).thenReturn(Optional.empty());
        when(deviceRepo.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Device device = service.ensureDevice(compositeId, null);

        assertEquals("S01", device.getSystem());
        assertEquals("L02", device.getLayer());
        assertEquals("esp32-01", device.getDeviceId());
        assertEquals(compositeId, device.getCompositeId());
    }
}

