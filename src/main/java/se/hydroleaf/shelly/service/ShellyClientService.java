package se.hydroleaf.shelly.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import se.hydroleaf.shelly.dto.ShellySwitchStatus;
import se.hydroleaf.shelly.exception.ShellyException;
import se.hydroleaf.shelly.model.ShellyDeviceConfig;

import java.time.Duration;

@Service
public class ShellyClientService {

    private static final Logger log = LoggerFactory.getLogger(ShellyClientService.class);

    private final RestTemplate restTemplate;

    public ShellyClientService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    public void turnOn(ShellyDeviceConfig device) {
        sendSwitchCommand(device, true);
    }

    public void turnOff(ShellyDeviceConfig device) {
        sendSwitchCommand(device, false);
    }

    public boolean toggle(ShellyDeviceConfig device) {
        boolean currentStatus = getStatus(device);
        if (currentStatus) {
            turnOff(device);
            return false;
        }
        turnOn(device);
        return true;
    }

    public boolean getStatus(ShellyDeviceConfig device) {
        String url = String.format("http://%s/rpc/Switch.Get?id=0", device.getIp());
        try {
            ResponseEntity<ShellySwitchStatus> response = restTemplate.getForEntity(url, ShellySwitchStatus.class);
            ShellySwitchStatus body = response.getBody();
            if (body == null) {
                throw new ShellyException(device.getId(), device.getIp(), "Empty response from Shelly device");
            }
            return body.isOutput();
        } catch (RestClientException e) {
            throw new ShellyException(device.getId(), device.getIp(), e.getMessage(), e);
        }
    }

    private void sendSwitchCommand(ShellyDeviceConfig device, boolean turnOn) {
        String url = String.format("http://%s/rpc/Switch.Set?id=0&on=%s", device.getIp(), turnOn);
        try {
            restTemplate.getForEntity(url, Void.class);
        } catch (RestClientException e) {
            log.warn("Failed to change state for device {} at {}", device.getId(), device.getIp());
            throw new ShellyException(device.getId(), device.getIp(), e.getMessage(), e);
        }
    }
}
