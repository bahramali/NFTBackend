package se.hydroleaf.shelly.registry;

import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import se.hydroleaf.shelly.model.Rack;
import se.hydroleaf.shelly.model.Room;
import se.hydroleaf.shelly.model.SocketDevice;

@Component
public class ShellyRegistry {

    private final Map<String, Room> roomsById = new ConcurrentHashMap<>();
    private final Map<String, Rack> racksById = new ConcurrentHashMap<>();
    private final Map<String, SocketDevice> socketsById = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        Room mainRoom = Room.builder().id("MAIN_ROOM").name("Main Room").build();
        roomsById.put(mainRoom.getId(), mainRoom);

        Rack rack01 = Rack.builder().id("RACK_01").name("Rack 01").roomId(mainRoom.getId()).build();
        Rack rack02 = Rack.builder().id("RACK_02").name("Rack 02").roomId(mainRoom.getId()).build();
        racksById.put(rack01.getId(), rack01);
        racksById.put(rack02.getId(), rack02);

        registerSocket(SocketDevice.builder()
                .id("PS01L02")
                .name("PS01L02")
                .rackId(rack01.getId())
                .ip("192.168.8.45")
                .relayIndex(0)
                .build());
        registerSocket(SocketDevice.builder()
                .id("PS01L03")
                .name("PS01L03")
                .rackId(rack01.getId())
                .ip("192.168.8.46")
                .relayIndex(0)
                .build());
        registerSocket(SocketDevice.builder()
                .id("PS01L04")
                .name("PS01L04")
                .rackId(rack01.getId())
                .ip("192.168.8.47")
                .relayIndex(0)
                .build());
        registerSocket(SocketDevice.builder()
                .id("PS01L05")
                .name("PS01L05")
                .rackId(rack01.getId())
                .ip("192.168.8.48")
                .relayIndex(0)
                .build());
    }

    public Collection<Room> getRooms() {
        return Collections.unmodifiableCollection(sortedRooms());
    }

    public List<Rack> getRacksByRoom(String roomId) {
        return racksById.values().stream()
                .filter(rack -> rack.getRoomId().equals(roomId))
                .sorted(Comparator.comparing(Rack::getId))
                .toList();
    }

    public List<Rack> getRacks() {
        return racksById.values().stream()
                .sorted(Comparator.comparing(Rack::getId))
                .toList();
    }

    public List<SocketDevice> getSocketsByRack(String rackId) {
        return socketsById.values().stream()
                .filter(socket -> socket.getRackId().equals(rackId))
                .sorted(Comparator.comparing(SocketDevice::getId))
                .toList();
    }

    public Collection<SocketDevice> getAllSockets() {
        return Collections.unmodifiableCollection(sortedSockets());
    }

    public Optional<Room> getRoom(String roomId) {
        return Optional.ofNullable(roomsById.get(roomId));
    }

    public Optional<Rack> getRack(String rackId) {
        return Optional.ofNullable(racksById.get(rackId));
    }

    public Optional<SocketDevice> getSocket(String socketId) {
        return Optional.ofNullable(socketsById.get(socketId));
    }

    private void registerSocket(SocketDevice socket) {
        socketsById.put(socket.getId(), socket);
    }

    private List<Room> sortedRooms() {
        return roomsById.values().stream()
                .sorted(Comparator.comparing(Room::getId))
                .collect(Collectors.toList());
    }

    private List<SocketDevice> sortedSockets() {
        return socketsById.values().stream()
                .sorted(Comparator.comparing(SocketDevice::getId))
                .collect(Collectors.toList());
    }
}
