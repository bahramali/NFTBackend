package se.hydroleaf.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.hydroleaf.dto.LiveNowSnapshot;
import se.hydroleaf.dto.LayerActuatorStatus;
import se.hydroleaf.dto.GrowSensorSummary;
import se.hydroleaf.dto.SystemSnapshot;
import se.hydroleaf.dto.WaterTankSummary;
import se.hydroleaf.dto.SystemActuatorStatus;
import se.hydroleaf.dto.StatusAllAverageResponse;
import se.hydroleaf.dto.StatusAverageResponse;
import se.hydroleaf.model.Device;
import se.hydroleaf.repository.ActuatorStatusRepository;
import se.hydroleaf.repository.AverageResult;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.SensorDataRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.function.Function;

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
        Map<String, Map<String, SystemSnapshot.LayerSnapshot>> systemLayers = new HashMap<>();
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

            Map<String, SystemSnapshot.LayerSnapshot> layers = systemLayers.computeIfAbsent(system, s -> new HashMap<>());
            layers.computeIfAbsent(layer, l -> {
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

                return new SystemSnapshot.LayerSnapshot(
                        layer,
                        Instant.now(),
                        actuator,
                        water,
                        environment
                );
            });
        }

        Map<String, SystemSnapshot> result = new HashMap<>();
        for (Map.Entry<String, Map<String, SystemSnapshot.LayerSnapshot>> entry : systemLayers.entrySet()) {
            List<SystemSnapshot.LayerSnapshot> layers = new ArrayList<>(entry.getValue().values());
            Instant lastUpdate = layers.stream()
                    .map(SystemSnapshot.LayerSnapshot::lastUpdate)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
            StatusAverageResponse airPump = aggregate(layers, l -> l.actuators().airPump());
            SystemActuatorStatus actuators = new SystemActuatorStatus(airPump);
            WaterTankSummary water = new WaterTankSummary(
                    aggregate(layers, l -> l.water().waterTemperature()),
                    aggregate(layers, l -> l.water().dissolvedOxygen()),
                    aggregate(layers, l -> l.water().pH()),
                    aggregate(layers, l -> l.water().electricalConductivity())
            );
            GrowSensorSummary environment = new GrowSensorSummary(
                    aggregate(layers, l -> l.environment().light()),
                    aggregate(layers, l -> l.environment().humidity()),
                    aggregate(layers, l -> l.environment().airTemperature())
            );
            result.put(entry.getKey(), new SystemSnapshot(lastUpdate, actuators, water, environment, layers));
        }
        return new LiveNowSnapshot(result);
    }

    private StatusAverageResponse aggregate(List<SystemSnapshot.LayerSnapshot> layers,
                                            Function<SystemSnapshot.LayerSnapshot, StatusAverageResponse> extractor) {
        double sum = 0.0;
        long count = 0L;
        String unit = null;
        for (SystemSnapshot.LayerSnapshot layer : layers) {
            StatusAverageResponse res = extractor.apply(layer);
            if (res == null) {
                continue;
            }
            if (unit == null) {
                unit = res.unit();
            }
            if (res.average() != null) {
                sum += res.average() * res.deviceCount();
            }
            count += res.deviceCount();
        }
        Double avg = count > 0 ? Math.round((sum / count) * 10.0) / 10.0 : null;
        return new StatusAverageResponse(avg, unit, count);
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
