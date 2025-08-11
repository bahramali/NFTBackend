package se.hydroleaf.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.hydroleaf.dto.StatusAllAverageResponse;
import se.hydroleaf.dto.StatusAverageResponse;
import se.hydroleaf.repository.AverageResult;
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

    public StatusService(SensorDataRepository sensorDataRepository,
                         OxygenPumpStatusRepository oxygenPumpStatusRepository) {
        this.sensorDataRepository = sensorDataRepository;
        this.oxygenPumpStatusRepository = oxygenPumpStatusRepository;
    }

    public StatusAverageResponse getAverage(String system, String layer, String sensorType) {
        String normalizedType = sensorType != null ? sensorType.toLowerCase() : null;
        AverageResult result;
        if (isOxygenPump(normalizedType)) {
            result = oxygenPumpStatusRepository.getLatestAverage(system, layer);
            log.info("oxygenPumpStatusRepository.getLatestAverage({}, {})= {}",system,layer,result);
        } else {
            result = sensorDataRepository.getLatestAverage(system, layer, normalizedType);
            log.info("sensorDataRepository.getLatestAverage({}, {},{})= {}",system,layer,normalizedType,result);
        }
        Double avg = result != null ? result.getAverage() : null;
        long count = result != null && result.getCount() != null ? result.getCount() : 0L;
        return new StatusAverageResponse(avg, count);
    }

    public StatusAllAverageResponse getAllAverages(String system, String layer) {
        List<String> sensorTypes = List.of("light", "humidity", "temperature", "dissolvedOxygen", "airpump");
        Map<String, StatusAverageResponse> responses = new HashMap<>();
        for (String type : sensorTypes) {
            responses.put(type, getAverage(system, layer, type));
        }
        return new StatusAllAverageResponse(
                responses.get("light"),
                responses.get("humidity"),
                responses.get("temperature"),
                responses.get("dissolvedOxygen"),
                responses.get("airpump")
        );
    }

    private boolean isOxygenPump(String sensorType) {
        if (sensorType == null) {
            return false;
        }
        return sensorType.equals("airpump");
    }
}
