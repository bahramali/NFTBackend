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
import se.hydroleaf.repository.LatestSensorValueAggregationRepository;
import se.hydroleaf.repository.dto.LiveNowRow;
import se.hydroleaf.model.DeviceType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
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
 * and then assembles the snapshot in-memory. Sensor readings are sourced from
 * the materialized {@code latest_sensor_value} table populated by database
 * triggers.</p>
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class StatusService {

    private static final List<DeviceType> SENSOR_TYPES = List.of(
            DeviceType.LIGHT,
            DeviceType.HUMIDITY,
            DeviceType.TEMPERATURE,
            DeviceType.DISSOLVED_OXYGEN,
            DeviceType.DISSOLVED_TEMP,
            DeviceType.PH,
            DeviceType.DISSOLVED_EC,
            DeviceType.DISSOLVED_TDS
    );

    private static final List<DeviceType> ACTUATOR_TYPES = List.of(DeviceType.AIR_PUMP);

    private final LatestSensorValueAggregationRepository sensorReadingRepository;
    private final ActuatorStatusRepository actuatorStatusRepository;

    public StatusService(LatestSensorValueAggregationRepository sensorReadingRepository,
                         ActuatorStatusRepository actuatorStatusRepository) {
        this.sensorReadingRepository = sensorReadingRepository;
        this.actuatorStatusRepository = actuatorStatusRepository;
    }

    public StatusAverageResponse getAverage(String system, String layer, String sensorType) {
        DeviceType type = DeviceType.fromName(sensorType);
        if (type == null) {
            return new StatusAverageResponse(null, null, 0L);
        }
        String unit = type.getUnit();

        if (type.isActuator()) {
            List<LiveNowRow> rows = actuatorStatusRepository.fetchLatestActuatorAverages(List.of(type.getName()));
            LiveNowRow row = rows.stream()
                    .filter(r -> r.system() != null && r.layer() != null
                            && system.equalsIgnoreCase(r.system())
                            && layer.equalsIgnoreCase(r.layer()))
                    .findFirst().orElse(null);
            long count = row != null && row.getDeviceCount() != null ? row.getDeviceCount() : 0L;
            Double avg = row != null && row.getAvgValue() != null && count > 0 ? row.getAvgValue() : null;
            String resultUnit = row != null && row.unit() != null ? row.unit() : unit;
            return new StatusAverageResponse(avg, resultUnit, count);
        } else {
            List<LiveNowRow> rows = sensorReadingRepository.fetchLatestSensorAverages(List.of(type.getName()));
            LiveNowRow row = rows.stream()
                    .filter(r -> r.system() != null && r.layer() != null
                            && system.equalsIgnoreCase(r.system())
                            && layer.equalsIgnoreCase(r.layer()))
                    .findFirst().orElse(null);
            long count = row != null && row.getDeviceCount() != null ? row.getDeviceCount() : 0L;
            Double avg = row != null && row.getAvgValue() != null && count > 0
                    ? Math.round(row.getAvgValue() * 10.0) / 10.0
                    : null;
            String resultUnit = row != null && row.unit() != null ? row.unit() : unit;
            return new StatusAverageResponse(avg, resultUnit, count);
        }
    }

    public StatusAllAverageResponse getAllAverages(String system, String layer) {
        List<LiveNowRow> sensorRows = sensorReadingRepository.fetchLatestSensorAverages(
                SENSOR_TYPES.stream().map(DeviceType::getName).toList());
        List<LiveNowRow> actuatorRows = actuatorStatusRepository.fetchLatestActuatorAverages(
                ACTUATOR_TYPES.stream().map(DeviceType::getName).toList());

        Map<String, LiveNowRow> sensorMap = sensorRows.stream()
                .filter(r -> r.system() != null && r.layer() != null
                        && system.equalsIgnoreCase(r.system())
                        && layer.equalsIgnoreCase(r.layer()))
                .collect(Collectors.toMap(LiveNowRow::sensorType, r -> r));
        Map<String, LiveNowRow> actuatorMap = actuatorRows.stream()
                .filter(r -> r.system() != null && r.layer() != null
                        && system.equalsIgnoreCase(r.system())
                        && layer.equalsIgnoreCase(r.layer()))
                .collect(Collectors.toMap(LiveNowRow::sensorType, r -> r));

        Map<DeviceType, StatusAverageResponse> responses = new EnumMap<>(DeviceType.class);
        for (DeviceType type : SENSOR_TYPES) {
            LiveNowRow row = sensorMap.get(type.getName());
            long count = row != null && row.getDeviceCount() != null ? row.getDeviceCount() : 0L;
            Double avg = row != null && row.getAvgValue() != null && count > 0
                    ? Math.round(row.getAvgValue() * 10.0) / 10.0
                    : null;
            String unit = row != null && row.unit() != null ? row.unit() : type.getUnit();
            responses.put(type, new StatusAverageResponse(avg, unit, count));
        }
        for (DeviceType type : ACTUATOR_TYPES) {
            LiveNowRow row = actuatorMap.get(type.getName());
            long count = row != null && row.getDeviceCount() != null ? row.getDeviceCount() : 0L;
            Double avg = row != null && row.getAvgValue() != null && count > 0 ? row.getAvgValue() : null;
            String unit = row != null && row.unit() != null ? row.unit() : type.getUnit();
            responses.put(type, new StatusAverageResponse(avg, unit, count));
        }

        Map<String, StatusAverageResponse> growSensors = Map.of(
                DeviceType.LIGHT.getName(), responses.get(DeviceType.LIGHT),
                DeviceType.HUMIDITY.getName(), responses.get(DeviceType.HUMIDITY),
                DeviceType.TEMPERATURE.getName(), responses.get(DeviceType.TEMPERATURE)
        );
        Map<String, StatusAverageResponse> waterTank = Map.ofEntries(
                Map.entry(DeviceType.DISSOLVED_TEMP.getName(), responses.get(DeviceType.DISSOLVED_TEMP)),
                Map.entry(DeviceType.DISSOLVED_OXYGEN.getName(), responses.get(DeviceType.DISSOLVED_OXYGEN)),
                Map.entry(DeviceType.PH.getName(), responses.get(DeviceType.PH)),
                Map.entry(DeviceType.DISSOLVED_EC.getName(), responses.get(DeviceType.DISSOLVED_EC)),
                Map.entry(DeviceType.DISSOLVED_TDS.getName(), responses.get(DeviceType.DISSOLVED_TDS))
        );
        DeviceType oxygenPumpType = ACTUATOR_TYPES.get(0);
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
        List<LiveNowRow> sensorRows = sensorReadingRepository.fetchLatestSensorAverages(
                SENSOR_TYPES.stream().map(DeviceType::getName).toList());
        List<LiveNowRow> actuatorRows = actuatorStatusRepository.fetchLatestActuatorAverages(
                ACTUATOR_TYPES.stream().map(DeviceType::getName).toList());

        Map<String, SystemData> systems = Stream
                // Sequential streams are sufficient here; reintroduce parallelism only with proper benchmarks
                .concat(sensorRows.stream(), actuatorRows.stream())
                .filter(r -> r.system() != null && !r.system().isBlank()
                        && r.layer() != null && !r.layer().isBlank())
                .collect(Collectors.groupingBy(
                        LiveNowRow::system,
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
        Map<DeviceType, SumCount> totals = new EnumMap<>(DeviceType.class);

        void accumulate(LiveNowRow row) {
            Instant time = row.getRecordTime();
            if (time != null) {
                lastUpdate = latest(lastUpdate, time);
            }

            LayerData layer = layers.computeIfAbsent(row.layer(), l -> new LayerData());
            if (time != null) {
                layer.lastUpdate = latest(layer.lastUpdate, time);
            }

            DeviceType type = DeviceType.fromName(row.sensorType());
            if (type == null) {
                return;
            }

            Double avg = row.getAvgValue();
            Long deviceCount = row.getDeviceCount();
            if (avg == null || deviceCount == null) {
                log.warn("Skipping accumulation for system {} layer {} type {} due to null avg or device count", row.system(), row.layer(), row.sensorType());
                return;
            }

            boolean actuator = type.isActuator();
            if (!actuator) {
                avg = Math.round(avg * 10.0) / 10.0;
            }
            long count = deviceCount;
            String unit = row.unit() != null ? row.unit() : type.getUnit();

            StatusAverageResponse resp = new StatusAverageResponse(avg, unit, count);
            layer.values.put(type, resp);

            SumCount sc = totals.computeIfAbsent(type, t -> {
                SumCount s = new SumCount();
                s.unit = unit;
                return s;
            });
            sc.unit = unit;
            sc.sum += avg * count;
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
                ActuatorStatusSummary layerActuators = new ActuatorStatusSummary(data.values.get(DeviceType.AIR_PUMP));
                WaterTankSummary layerWater = new WaterTankSummary(
                        data.values.get(DeviceType.DISSOLVED_TEMP),
                        data.values.get(DeviceType.DISSOLVED_OXYGEN),
                        data.values.get(DeviceType.PH),
                        data.values.get(DeviceType.DISSOLVED_EC),
                        data.values.get(DeviceType.DISSOLVED_TDS)
                );
                GrowSensorSummary layerEnv = new GrowSensorSummary(
                        data.values.get(DeviceType.LIGHT),
                        data.values.get(DeviceType.HUMIDITY),
                        data.values.get(DeviceType.TEMPERATURE)
                );
                layerSnapshots.add(new SystemSnapshot.LayerSnapshot(layerId, data.lastUpdate,
                        layerActuators, layerWater, layerEnv));
            });

            ActuatorStatusSummary systemActuators = new ActuatorStatusSummary(aggregate(totals.get(DeviceType.AIR_PUMP)));
            WaterTankSummary systemWater = new WaterTankSummary(
                    aggregate(totals.get(DeviceType.DISSOLVED_TEMP)),
                    aggregate(totals.get(DeviceType.DISSOLVED_OXYGEN)),
                    aggregate(totals.get(DeviceType.PH)),
                    aggregate(totals.get(DeviceType.DISSOLVED_EC)),
                    aggregate(totals.get(DeviceType.DISSOLVED_TDS))
            );
            GrowSensorSummary systemEnv = new GrowSensorSummary(
                    aggregate(totals.get(DeviceType.LIGHT)),
                    aggregate(totals.get(DeviceType.HUMIDITY)),
                    aggregate(totals.get(DeviceType.TEMPERATURE))
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

    private static class LayerData {
        Instant lastUpdate;
        Map<DeviceType, StatusAverageResponse> values = new EnumMap<>(DeviceType.class);
    }

    private static class SumCount {
        double sum;
        long count;
        String unit;
    }
}

