package se.hydroleaf.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.hydroleaf.repository.dto.summary.ActuatorStatusSummary;
import se.hydroleaf.repository.dto.summary.GrowSensorSummary;
import se.hydroleaf.repository.dto.summary.StatusAllAverageResponse;
import se.hydroleaf.repository.dto.summary.StatusAverageResponse;
import se.hydroleaf.repository.dto.summary.WaterTankSummary;
import se.hydroleaf.repository.ActuatorStatusRepository;
import se.hydroleaf.repository.LatestSensorValueAggregationRepository;
import se.hydroleaf.repository.dto.snapshot.LiveNowRow;
import se.hydroleaf.model.DeviceType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
}

