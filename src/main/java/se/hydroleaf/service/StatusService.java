package se.hydroleaf.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.hydroleaf.dto.LiveNowSnapshot;
import se.hydroleaf.dto.LayerActuatorStatus;
import se.hydroleaf.dto.GrowSensorSummary;
import se.hydroleaf.dto.WaterTankSummary;
import se.hydroleaf.dto.StatusAllAverageResponse;
import se.hydroleaf.dto.StatusAverageResponse;
import se.hydroleaf.model.Device;
import se.hydroleaf.repository.AverageResult;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.OxygenPumpStatusRepository;
import se.hydroleaf.repository.SensorDataRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class StatusService {

    private final SensorDataRepository sensorDataRepository;
    private final OxygenPumpStatusRepository oxygenPumpStatusRepository;
    private final DeviceRepository deviceRepository;

    public StatusService(SensorDataRepository sensorDataRepository,
                         OxygenPumpStatusRepository oxygenPumpStatusRepository,
                         DeviceRepository deviceRepository) {
        this.sensorDataRepository = sensorDataRepository;
        this.oxygenPumpStatusRepository = oxygenPumpStatusRepository;
        this.deviceRepository = deviceRepository;
    }

    public StatusAverageResponse getAverage(String system, String layer, String sensorType) {
        AverageResult result;
        if (isOxygenPump(sensorType)) {
            result = oxygenPumpStatusRepository.getLatestPumpAverage(system, layer);
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
                        LayerActuatorStatus actuator = new LayerActuatorStatus(getAverage(system, layer, "airPump"));
                        GrowSensorSummary growSensors = new GrowSensorSummary(
                                getAverage(system, layer, "light"),
                                getAverage(system, layer, "humidity"),
                                getAverage(system, layer, "temperature")
                        );
                        WaterTankSummary waterTank = new WaterTankSummary(
                                getAverage(system, layer, "dissolvedTemp"),
                                getAverage(system, layer, "dissolvedOxygen"),
                                getAverage(system, layer, "dissolvedPH"),
                                getAverage(system, layer, "dissolvedEC")
                        );
                        return new LiveNowSnapshot.LayerSnapshot(actuator, growSensors, waterTank);
                    });
        }
        return new LiveNowSnapshot(result);
    }

    private boolean isOxygenPump(String sensorType) {
        if (sensorType == null) {
            return false;
        }
        return sensorType.equalsIgnoreCase("airPump") || sensorType.equalsIgnoreCase("airpump");
    }
}
