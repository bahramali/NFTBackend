package se.hydroleaf.service;

import org.springframework.stereotype.Service;
import se.hydroleaf.dto.StatusAllAverageResponse;
import se.hydroleaf.dto.StatusAverageResponse;
import se.hydroleaf.repository.AverageResult;
import se.hydroleaf.repository.OxygenPumpStatusRepository;
import se.hydroleaf.repository.SensorDataRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatusService {

    private final SensorDataRepository sensorDataRepository;
    private final OxygenPumpStatusRepository oxygenPumpStatusRepository;

    public StatusService(SensorDataRepository sensorDataRepository,
                         OxygenPumpStatusRepository oxygenPumpStatusRepository) {
        this.sensorDataRepository = sensorDataRepository;
        this.oxygenPumpStatusRepository = oxygenPumpStatusRepository;
    }

    public StatusAverageResponse getAverage(String system, String layer, String sensorType) {
        AverageResult result;
        if (isOxygenPump(sensorType)) {
            result = oxygenPumpStatusRepository.getLatestAverage(system, layer);
        } else {
            result = sensorDataRepository.getLatestAverage(system, layer, sensorType);
        }
        Double avg = result != null ? result.getAverage() : null;
        long count = result != null && result.getCount() != null ? result.getCount() : 0L;
        return new StatusAverageResponse(avg, count);
    }

    public StatusAllAverageResponse getAllAverages(String system, String layer) {
        List<String> sensorTypes = List.of("lux", "humidity", "temperature", "do", "airpump");
        Map<String, StatusAverageResponse> responses = new HashMap<>();
        for (String type : sensorTypes) {
            responses.put(type, getAverage(system, layer, type));
        }
        return new StatusAllAverageResponse(
                responses.get("lux"),
                responses.get("humidity"),
                responses.get("temperature"),
                responses.get("do"),
                responses.get("airpump")
        );
    }

    private boolean isOxygenPump(String sensorType) {
        if (sensorType == null) {
            return false;
        }
        String type = sensorType.toLowerCase();
        return type.equals("oxygenpump") || type.equals("oxygen-pump") ||
                type.equals("oxygenpumpstatus") || type.equals("airpump");
    }
}
