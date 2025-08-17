package se.hydroleaf.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.hydroleaf.dto.snapshot.LiveNowSnapshot;
import se.hydroleaf.dto.snapshot.SystemSnapshot;
import se.hydroleaf.dto.summary.ActuatorStatusSummary;
import se.hydroleaf.dto.summary.GrowSensorSummary;
import se.hydroleaf.dto.summary.StatusAllAverageResponse;
import se.hydroleaf.dto.summary.StatusAverageResponse;
import se.hydroleaf.dto.summary.WaterTankSummary;
import se.hydroleaf.repository.ActuatorStatusRepository;
import se.hydroleaf.repository.AverageCount;
import se.hydroleaf.repository.SensorDataRepository;
import se.hydroleaf.repository.dto.LiveNowRow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service providing aggregated status information about sensors and actuators.
 *
 * <p>The previous implementation queried one system/layer at a time. The new
 * approach performs two bulk queries – one for sensors and one for actuators –
 * and then assembles the snapshot in-memory.</p>
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class StatusService {

    private static final List<String> SENSOR_TYPES = List.of(
            "light",
            "humidity",
            "temperature",
            "dissolvedOxygen",
            "dissolvedTemp",
            "pH",
            "dissolvedEC",
            "dissolvedTDS"
    );

    private static final List<String> ACTUATOR_TYPES = List.of("airPump");

    private final SensorDataRepository sensorDataRepository;
    private final ActuatorStatusRepository actuatorStatusRepository;

    public StatusService(SensorDataRepository sensorDataRepository,
                         ActuatorStatusRepository actuatorStatusRepository) {
        this.sensorDataRepository = sensorDataRepository;
        this.actuatorStatusRepository = actuatorStatusRepository;
    }

    public StatusAverageResponse getAverage(String system, String layer, String sensorType) {
        String unit = unitOf(sensorType);

        if (isActuator(sensorType)) {
            AverageCount ac = actuatorStatusRepository.getLatestActuatorAverage(system, layer, sensorType);
            long count = ac != null ? ac.getCount() : 0L;
            Double avg = ac != null && count > 0 ? ac.getAverage() : null;
            return new StatusAverageResponse(avg, unit, count);
        } else {
            AverageCount ac = sensorDataRepository.getLatestAverage(system, layer, sensorType);
            long count = ac != null ? ac.getCount() : 0L;
            Double avg = ac != null && count > 0 ? Math.round(ac.getAverage() * 10.0) / 10.0 : null;
            return new StatusAverageResponse(avg, unit, count);
        }
    }

    public StatusAllAverageResponse getAllAverages(String system, String layer) {
        String oxygenPumpType = "airPump";
        List<String> sensorTypes = new ArrayList<>(SENSOR_TYPES);
        sensorTypes.add(oxygenPumpType);
        Map<String, StatusAverageResponse> responses = new HashMap<>();
        for (String type : sensorTypes) {
            responses.put(type, getAverage(system, layer, type));
        }
        Map<String, StatusAverageResponse> growSensors = Map.of(
                "light", responses.get("light"),
                "humidity", responses.get("humidity"),
                "temperature", responses.get("temperature")
        );
        Map<String, StatusAverageResponse> waterTank = Map.ofEntries(
                Map.entry("dissolvedTemp", responses.get("dissolvedTemp")),
                Map.entry("dissolvedOxygen", responses.get("dissolvedOxygen")),
                Map.entry("pH", responses.get("pH")),
                Map.entry("dissolvedEC", responses.get("dissolvedEC")),
                Map.entry("dissolvedTDS", responses.get("dissolvedTDS"))
        );
        return new StatusAllAverageResponse(
                growSensors,
                waterTank,
                responses.get(oxygenPumpType)
        );
    }

    /**
     * Collects the latest readings for all systems and layers and assembles a snapshot.
     */
    public LiveNowSnapshot getLiveNowSnapshot() {
        List<LiveNowRow> sensorRows = sensorDataRepository.fetchLatestSensorAverages(SENSOR_TYPES);
        List<LiveNowRow> actuatorRows = actuatorStatusRepository.fetchLatestActuatorAverages(ACTUATOR_TYPES);

        Map<String, Map<String, LayerData>> aggregated = new HashMap<>();
        for (LiveNowRow row : sensorRows) {
            accumulate(aggregated, row, false);
        }
        for (LiveNowRow row : actuatorRows) {
            accumulate(aggregated, row, true);
        }

        return assembleSnapshot(aggregated);
    }

    private void accumulate(Map<String, Map<String, LayerData>> acc, LiveNowRow row, boolean actuator) {
        String system = row.system();
        String layer = row.layer();
        if (system == null || system.isBlank() || layer == null || layer.isBlank()) {
            return;
        }

        Map<String, LayerData> layers = acc.computeIfAbsent(system, s -> new HashMap<>());
        LayerData data = layers.computeIfAbsent(layer, l -> new LayerData());

        Instant time = row.recordTime();
        if (time != null && (data.lastUpdate == null || time.isAfter(data.lastUpdate))) {
            data.lastUpdate = time;
        }

        Double avg = row.avgValue();
        if (!actuator && avg != null) {
            avg = Math.round(avg * 10.0) / 10.0;
        }
        long count = row.deviceCount() != null ? row.deviceCount() : 0L;
        String unit = row.unit() != null ? row.unit() : unitOf(row.sensorType());

        data.values.put(row.sensorType(), new StatusAverageResponse(avg, unit, count));
    }

    private LiveNowSnapshot assembleSnapshot(Map<String, Map<String, LayerData>> acc) {
        Map<String, SystemSnapshot> systems = new HashMap<>();

        for (Map.Entry<String, Map<String, LayerData>> systemEntry : acc.entrySet()) {
            String systemId = systemEntry.getKey();
            Map<String, LayerData> layersMap = systemEntry.getValue();

            List<SystemSnapshot.LayerSnapshot> layerSnapshots = new ArrayList<>();
            Map<String, SumCount> totals = new HashMap<>();
            Instant systemLast = null;

            for (Map.Entry<String, LayerData> layerEntry : layersMap.entrySet()) {
                String layerId = layerEntry.getKey();
                LayerData data = layerEntry.getValue();
                systemLast = latest(systemLast, data.lastUpdate);

                ActuatorStatusSummary layerActuators = new ActuatorStatusSummary(data.values.get("airPump"));
                WaterTankSummary layerWater = new WaterTankSummary(
                        data.values.get("dissolvedTemp"),
                        data.values.get("dissolvedOxygen"),
                        data.values.get("pH"),
                        data.values.get("dissolvedEC"),
                        data.values.get("dissolvedTDS")
                );
                GrowSensorSummary layerEnv = new GrowSensorSummary(
                        data.values.get("light"),
                        data.values.get("humidity"),
                        data.values.get("temperature")
                );

                layerSnapshots.add(new SystemSnapshot.LayerSnapshot(layerId, data.lastUpdate,
                        layerActuators, layerWater, layerEnv));

                data.values.forEach((type, res) -> {
                    if (res == null) {
                        return;
                    }
                    SumCount sc = totals.computeIfAbsent(type, t -> new SumCount());
                    sc.unit = res.unit();
                    if (res.average() != null) {
                        sc.sum += res.average() * res.deviceCount();
                    }
                    sc.count += res.deviceCount();
                });
            }

            ActuatorStatusSummary systemActuators = new ActuatorStatusSummary(aggregate(totals.get("airPump")));
            WaterTankSummary systemWater = new WaterTankSummary(
                    aggregate(totals.get("dissolvedTemp")),
                    aggregate(totals.get("dissolvedOxygen")),
                    aggregate(totals.get("pH")),
                    aggregate(totals.get("dissolvedEC")),
                    aggregate(totals.get("dissolvedTDS"))
            );
            GrowSensorSummary systemEnv = new GrowSensorSummary(
                    aggregate(totals.get("light")),
                    aggregate(totals.get("humidity")),
                    aggregate(totals.get("temperature"))
            );

            systems.put(systemId, new SystemSnapshot(systemLast, systemActuators, systemWater, systemEnv, layerSnapshots));
        }

        return new LiveNowSnapshot(systems);
    }

    private Instant latest(Instant a, Instant b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }

    private StatusAverageResponse aggregate(SumCount sc) {
        if (sc == null) {
            return null;
        }
        Double avg = sc.count > 0 ? Math.round((sc.sum / sc.count) * 10.0) / 10.0 : null;
        return new StatusAverageResponse(avg, sc.unit, sc.count);
    }

    private boolean isActuator(String sensorType) {
        if (sensorType == null) {
            return false;
        }
        return sensorType.equalsIgnoreCase("airPump") || sensorType.equalsIgnoreCase("airpump");
    }

    private String unitOf(String sensorType) {
        if (sensorType == null) {
            return null;
        }
        return switch (sensorType.toLowerCase()) {
            case "light" -> "lux";
            case "humidity" -> "%";
            case "temperature", "dissolvedtemp" -> "°C";
            case "dissolvedoxygen" -> "mg/L";
            case "ph" -> "pH";
            case "dissolvedec" -> "mS/cm";
            case "dissolvedtds" -> "ppm";
            case "airpump" -> "status";
            default -> null;
        };
    }

    private static class LayerData {
        Instant lastUpdate;
        Map<String, StatusAverageResponse> values = new HashMap<>();
    }

    private static class SumCount {
        double sum;
        long count;
        String unit;
    }
}

