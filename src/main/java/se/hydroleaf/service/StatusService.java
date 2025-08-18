package se.hydroleaf.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        Map<String, AverageCount> sensorAverages =
                sensorDataRepository.getLatestAverages(system, layer, SENSOR_TYPES);
        Map<String, AverageCount> actuatorAverages =
                actuatorStatusRepository.getLatestActuatorAverages(system, layer, ACTUATOR_TYPES);

        Map<String, StatusAverageResponse> responses = new HashMap<>();
        for (String type : SENSOR_TYPES) {
            AverageCount ac = sensorAverages.get(type);
            long count = ac != null ? ac.getCount() : 0L;
            Double avg = ac != null && count > 0 ? Math.round(ac.getAverage() * 10.0) / 10.0 : null;
            responses.put(type, new StatusAverageResponse(avg, unitOf(type), count));
        }
        for (String type : ACTUATOR_TYPES) {
            AverageCount ac = actuatorAverages.get(type);
            long count = ac != null ? ac.getCount() : 0L;
            Double avg = ac != null && count > 0 ? ac.getAverage() : null;
            responses.put(type, new StatusAverageResponse(avg, unitOf(type), count));
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
        String oxygenPumpType = ACTUATOR_TYPES.get(0);
        return new StatusAllAverageResponse(
                growSensors,
                waterTank,
                responses.get(oxygenPumpType)
        );
    }

    /**
     * Collects the latest readings for all systems and layers and assembles a snapshot.
     */
    @Cacheable(cacheNames = "liveNow", key = "'default'", condition = "@environment.getProperty('cache.liveNow.enabled','true') == 'true'")
    public LiveNowSnapshot getLiveNowSnapshot() {
        List<LiveNowRow> sensorRows = sensorDataRepository.fetchLatestSensorAverages(SENSOR_TYPES);
        List<LiveNowRow> actuatorRows = actuatorStatusRepository.fetchLatestActuatorAverages(ACTUATOR_TYPES);

        Map<String, SystemData> systems = Stream
                .concat(sensorRows.parallelStream(), actuatorRows.parallelStream())
                .filter(r -> r.getSystem() != null && !r.getSystem().isBlank()
                        && r.getLayer() != null && !r.getLayer().isBlank())
                .collect(Collectors.groupingBy(
                        LiveNowRow::getSystem,
                        Collector.of(SystemData::new, SystemData::accumulate, (a, b) -> {
                            a.merge(b);
                            return a;
                        })
                ));

        Map<String, SystemSnapshot> snapshots = systems.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toSnapshot()));

        return new LiveNowSnapshot(snapshots);
    }

    private class SystemData {
        Instant lastUpdate;
        Map<String, LayerData> layers = new HashMap<>();
        Map<String, SumCount> totals = new HashMap<>();

        void accumulate(LiveNowRow row) {
            Instant time = row.getRecordTime();
            if (time != null) {
                lastUpdate = latest(lastUpdate, time);
            }

            LayerData layer = layers.computeIfAbsent(row.getLayer(), l -> new LayerData());
            if (time != null) {
                layer.lastUpdate = latest(layer.lastUpdate, time);
            }

            Double avg = row.getAvgValue();
            boolean actuator = isActuator(row.getSensorType());
            if (!actuator && avg != null) {
                avg = Math.round(avg * 10.0) / 10.0;
            }
            long count = row.getDeviceCount() != null ? row.getDeviceCount() : 0L;
            String unit = row.getUnit() != null ? row.getUnit() : unitOf(row.getSensorType());

            StatusAverageResponse resp = new StatusAverageResponse(avg, unit, count);
            layer.values.put(row.getSensorType(), resp);

            SumCount sc = totals.computeIfAbsent(row.getSensorType(), t -> {
                SumCount s = new SumCount();
                s.unit = unit;
                return s;
            });
            sc.unit = unit;
            if (avg != null) {
                sc.sum += avg * count;
            }
            sc.count += count;
        }

        void merge(SystemData other) {
            lastUpdate = latest(lastUpdate, other.lastUpdate);
            other.layers.forEach((layerId, otherLayer) ->
                    layers.merge(layerId, otherLayer, (l1, l2) -> {
                        l1.lastUpdate = latest(l1.lastUpdate, l2.lastUpdate);
                        l1.values.putAll(l2.values);
                        return l1;
                    }));
            other.totals.forEach((type, scOther) ->
                    totals.merge(type, scOther, (sc1, sc2) -> {
                        sc1.sum += sc2.sum;
                        sc1.count += sc2.count;
                        if (sc1.unit == null) sc1.unit = sc2.unit;
                        return sc1;
                    }));
        }

        SystemSnapshot toSnapshot() {
            List<SystemSnapshot.LayerSnapshot> layerSnapshots = new ArrayList<>();
            layers.forEach((layerId, data) -> {
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
            });

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

            return new SystemSnapshot(lastUpdate, systemActuators, systemWater, systemEnv, layerSnapshots);
        }
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

