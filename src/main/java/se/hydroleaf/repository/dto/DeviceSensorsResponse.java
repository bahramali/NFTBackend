package se.hydroleaf.repository.dto;

import java.util.List;

public record DeviceSensorsResponse(
        String version,
        List<SystemInfo> systems,
        List<DeviceInfo> devices
) {
    public record SystemInfo(String id, List<String> layers) {}
    public record DeviceInfo(String systemId, String layerId, String deviceId, List<String> sensors) {}
}
