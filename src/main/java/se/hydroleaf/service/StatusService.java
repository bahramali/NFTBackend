package se.hydroleaf.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.hydroleaf.dto.LiveNowSnapshot;
import se.hydroleaf.dto.LayerActuatorStatus;
import se.hydroleaf.dto.LayerSensorSummary;
import se.hydroleaf.dto.StatusAllAverageResponse;
import se.hydroleaf.dto.StatusAverageResponse;
import se.hydroleaf.model.Device;
import se.hydroleaf.repository.ActuatorStatusRepository;
import se.hydroleaf.repository.AverageResult;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.SensorDataRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class StatusService {

    private final SensorDataRepository sensorDataRepository;
    private final ActuatorStatusRepository actuatorStatusRepository;
    private final DeviceRepository deviceRepository;

    public StatusService(SensorDataRepository sensorDataRepository,
                         ActuatorStatusRepository actuatorStatusRepository,
                         DeviceRepository deviceRepository) {
        this.sensorDataRepository = sensorDataRepository;
        this.actuatorStatusRepository = actuatorStatusRepository;
        this.deviceRepository = deviceRepository;
    }

    public StatusAverageResponse getAverage(String system, String layer, String sensorType) {
        AverageResult result;
        if (isActuator(sensorType)) {
            result = actuatorStatusRepository.getLatestActuatorAverage(system, layer, sensorType);
        } else {
            result = sensorDataRepository.getLatestAverage(system, layer, sensorType);
        }
        Double avg = (result != null && result.getAverage() != null) ? (double) Math.round(result.getAverage() * 10) / 10 : null;
        long count = result != null && result.getCount() != null ? result.getCount() : 0L;
        return new StatusAverageResponse(avg, count);
    }

    public StatusAllAverageResponse getAllAverages(String system, String layer) {
        String oxygenPumpType = "airPump";
        List<String> sensorTypes = List.of("light", "humidity", "temperature", "dissolvedOxygen", oxygenPumpType);
        Map<String, StatusAverageResponse> responses = new HashMap<>();
        for (String type : sensorTypes) {
            responses.put(type, getAverage(system, layer, type));
        }
        return new StatusAllAverageResponse(
                responses.get("light"),
                responses.get("humidity"),
                responses.get("temperature"),
                responses.get("dissolvedOxygen"),
                responses.get(oxygenPumpType)
        );
    }

    public LiveNowSnapshot getLiveNowSnapshot() {
        Map<String, Map<String, LiveNowSnapshot.LayerSnapshot>> result = new HashMap<>();
        List<Device> devices = deviceRepository.findAll();
        for (Device device : devices) {
            String system = device.getSystem();
            String layer = device.getLayer();

            if (system == null || system.isBlank()) {
                continue;
            }
            if (layer == null || layer.isBlank()) {
                continue;
            }

            result.computeIfAbsent(system, s -> new HashMap<>())
                    .computeIfAbsent(layer, l -> {
                        StatusAllAverageResponse all = getAllAverages(system, layer);
                        LayerActuatorStatus actuator = new LayerActuatorStatus(all.airpump());
                        LayerSensorSummary growSensors = new LayerSensorSummary(all.light(), all.humidity(), all.temperature(), null);
                        LayerSensorSummary waterTank = new LayerSensorSummary(null, null, null, all.dissolvedOxygen());
                        return new LiveNowSnapshot.LayerSnapshot(actuator, growSensors, waterTank);
                    });
        }
        return new LiveNowSnapshot(result);
    }

    private boolean isActuator(String sensorType) {
        if (sensorType == null) {
            return false;
        }
        return sensorType.equalsIgnoreCase("airPump") || sensorType.equalsIgnoreCase("airpump");
    }
}
