package se.hydroleaf.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.hydroleaf.dto.LiveNowSnapshot;
import se.hydroleaf.dto.LayerActuatorStatus;
import se.hydroleaf.dto.GrowSensorSummary;
import se.hydroleaf.dto.SystemSnapshot;
import se.hydroleaf.dto.WaterTankSummary;
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
        String unit = getUnit(sensorType);
        return new StatusAverageResponse(avg, unit, count);
    }

    public StatusAllAverageResponse getAllAverages(String system, String layer) {
        String oxygenPumpType = "airPump";
        List<String> sensorTypes = List.of(
                "light",
                "humidity",
                "airTemperature",
                "dissolvedOxygen",
                "waterTemperature",
                "pH",
                "electricalConductivity",
                oxygenPumpType
        );
        Map<String, StatusAverageResponse> responses = new HashMap<>();
        for (String type : sensorTypes) {
            responses.put(type, getAverage(system, layer, type));
        }
        Map<String, StatusAverageResponse> growSensors = Map.of(
                "light", responses.get("light"),
                "humidity", responses.get("humidity"),
                "airTemperature", responses.get("airTemperature")
        );
        Map<String, StatusAverageResponse> waterTank = Map.of(
                "waterTemperature", responses.get("waterTemperature"),
                "dissolvedOxygen", responses.get("dissolvedOxygen"),
                "pH", responses.get("pH"),
                "electricalConductivity", responses.get("electricalConductivity")
        );
        return new StatusAllAverageResponse(
                growSensors,
                waterTank,
                responses.get(oxygenPumpType)
        );
    }

    public LiveNowSnapshot getLiveNowSnapshot() {
        Map<String, SystemSnapshot> result = new HashMap<>();
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

            LayerActuatorStatus actuator = new LayerActuatorStatus(getAverage(system, layer, "airPump"));
            GrowSensorSummary environment = new GrowSensorSummary(
                    getAverage(system, layer, "light"),
                    getAverage(system, layer, "humidity"),
                    getAverage(system, layer, "airTemperature")
            );
            WaterTankSummary water = new WaterTankSummary(
                    getAverage(system, layer, "waterTemperature"),
                    getAverage(system, layer, "dissolvedOxygen"),
                    getAverage(system, layer, "pH"),
                    getAverage(system, layer, "electricalConductivity")
            );

            SystemSnapshot snapshot = new SystemSnapshot(java.time.Instant.now(), actuator, water, environment);
            result.put(system, snapshot);
        }
        return new LiveNowSnapshot(result);
    }

    private boolean isActuator(String sensorType) {
        if (sensorType == null) {
            return false;
        }
        return sensorType.equalsIgnoreCase("airPump") || sensorType.equalsIgnoreCase("airpump");
    }

    private String getUnit(String sensorType) {
        if (sensorType == null) {
            return null;
        }
        return switch (sensorType.toLowerCase()) {
            case "light" -> "lux";
            case "humidity" -> "%";
            case "airtemperature", "watertemperature" -> "°C";
            case "dissolvedoxygen" -> "mg/L";
            case "ph" -> "pH";
            case "electricalconductivity" -> "µS/cm";
            case "airpump" -> "status";
            default -> null;
        };
    }
}
