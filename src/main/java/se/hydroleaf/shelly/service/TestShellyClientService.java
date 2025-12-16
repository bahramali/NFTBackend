package se.hydroleaf.shelly.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import se.hydroleaf.shelly.dto.SocketStatusDTO;
import se.hydroleaf.shelly.model.SocketDevice;
import se.hydroleaf.shelly.registry.ShellyRegistry;

@Service
@Primary
@Profile("test")
public class TestShellyClientService implements ShellyClient {

    private final Map<String, Boolean> socketStates = new ConcurrentHashMap<>();

    public TestShellyClientService(ShellyRegistry registry) {
        registry.getAllSockets().forEach(device -> socketStates.putIfAbsent(device.getId(), false));
    }

    @Override
    public SocketStatusDTO getStatus(SocketDevice device) {
        boolean output = socketStates.getOrDefault(device.getId(), false);
        return SocketStatusDTO.builder()
                .socketId(device.getId())
                .output(output)
                .online(true)
                .lastUpdated(Instant.now())
                .build();
    }

    @Override
    public SocketStatusDTO turnOn(SocketDevice device) {
        socketStates.put(device.getId(), true);
        return getStatus(device);
    }

    @Override
    public SocketStatusDTO turnOff(SocketDevice device) {
        socketStates.put(device.getId(), false);
        return getStatus(device);
    }

    @Override
    public SocketStatusDTO toggle(SocketDevice device) {
        boolean next = !socketStates.getOrDefault(device.getId(), false);
        socketStates.put(device.getId(), next);
        return getStatus(device);
    }
}
