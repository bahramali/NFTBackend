package se.hydroleaf.service;

import org.springframework.stereotype.Service;
import se.hydroleaf.model.SensorConfig;
import se.hydroleaf.repository.SensorConfigRepository;

import java.util.List;
import java.util.Objects;

@Service
public class SensorConfigService {

    private final SensorConfigRepository repository;

    public SensorConfigService(SensorConfigRepository repository) {
        this.repository = repository;
    }

    public List<SensorConfig> getAll() {
        return repository.findAll();
    }

    public SensorConfig get(String sensorType) {
        return repository.findBySensorType(sensorType)
                .orElseThrow(() -> new IllegalArgumentException("Unknown sensor type: " + sensorType));
    }

    public SensorConfig create(SensorConfig config) {
        if (config.getMinValue() == null || config.getMaxValue() == null) {
            throw new IllegalArgumentException("Min and max values must be provided");
        }
        if (repository.existsBySensorType(config.getSensorType())) {
            throw new IllegalArgumentException("Sensor type already exists: " + config.getSensorType());
        }
        return repository.save(config);
    }

    public SensorConfig update(String sensorType, SensorConfig config) {
        SensorConfig existing = get(sensorType);
        existing.setMinValue(Objects.requireNonNull(config.getMinValue(), "Min value must be provided"));
        existing.setMaxValue(Objects.requireNonNull(config.getMaxValue(), "Max value must be provided"));
        existing.setDescription(config.getDescription());
        return repository.save(existing);
    }

    public void delete(String sensorType) {
        if (!repository.existsBySensorType(sensorType)) {
            throw new IllegalArgumentException("Unknown sensor type: " + sensorType);
        }
        repository.deleteBySensorType(sensorType);
    }
}

