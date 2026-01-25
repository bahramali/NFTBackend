package se.hydroleaf.repository.dto;

import java.util.List;

public record DeviceSensorsResponse(
        String version,
        List<SystemInfo> systems,
        List<DeviceInfo> devices
) {
    public record SystemInfo(String id, List<String> layers, List<String> compositeIds) {}
    public record DeviceInfo(String systemId, String rackId, String layerId, String deviceId, List<String> sensors) {}
}
