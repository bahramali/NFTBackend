package se.hydroleaf.shelly.service;

import java.time.Duration;
import java.time.Instant;
import io.netty.handler.logging.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;
import se.hydroleaf.shelly.dto.ShellySwitchStatus;
import se.hydroleaf.shelly.dto.SocketStatusDTO;
import se.hydroleaf.shelly.exception.ShellyException;
import se.hydroleaf.shelly.model.SocketDevice;

@Service
public class ShellyClientService {

    private static final Logger log = LoggerFactory.getLogger(ShellyClientService.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public ShellyClientService(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(REQUEST_TIMEOUT)
                .wiretap("shelly-http", LogLevel.INFO, AdvancedByteBufFormat.TEXTUAL);
        this.webClient = builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    public SocketStatusDTO getStatus(SocketDevice device) {
        String url = statusUrl(device);
        try {
            ShellySwitchStatus body = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(ShellySwitchStatus.class)
                    .block(REQUEST_TIMEOUT);
            if (body == null) {
                throw new ShellyException(device.getId(), device.getIp(), "Empty response from Shelly device");
            }
            return SocketStatusDTO.builder()
                    .socketId(device.getId())
                    .output(body.isOutput())
                    .powerW(body.getActivePower())
                    .voltageV(body.getVoltage())
                    .online(true)
                    .lastUpdated(Instant.now())
                    .build();
        } catch (WebClientResponseException | WebClientRequestException ex) {
            throw wrapException(device, ex);
        }
    }

    public SocketStatusDTO turnOn(SocketDevice device) {
        sendSwitchCommand(device, true);
        return getStatus(device);
    }

    public SocketStatusDTO turnOff(SocketDevice device) {
        sendSwitchCommand(device, false);
        return getStatus(device);
    }

    public SocketStatusDTO toggle(SocketDevice device) {
        SocketStatusDTO current = getStatus(device);
        if (current.isOutput()) {
            return turnOff(device);
        }
        return turnOn(device);
    }

    private void sendSwitchCommand(SocketDevice device, boolean turnOn) {
        String url = switchUrl(device, turnOn);
        try {
            webClient.get().uri(url).retrieve().toBodilessEntity().block(REQUEST_TIMEOUT);
        } catch (WebClientResponseException | WebClientRequestException ex) {
            throw wrapException(device, ex);
        }
    }

    private ShellyException wrapException(SocketDevice device, Exception ex) {
        if (ex instanceof WebClientResponseException responseException) {
            HttpStatusCode statusCode = responseException.getStatusCode();
            log.warn("Shelly {} responded with {} for {}", device.getId(), statusCode, device.getIp());
            String statusText = statusCode instanceof HttpStatus httpStatus
                    ? httpStatus.getReasonPhrase()
                    : statusCode.toString();
            return new ShellyException(
                    device.getId(), device.getIp(), "Shelly responded with " + statusText, ex);
        }
        if (ex instanceof WebClientRequestException requestException) {
            log.warn("Shelly {} unreachable at {}: {}", device.getId(), device.getIp(), requestException.getMessage());
            return new ShellyException(device.getId(), device.getIp(), "Unable to reach Shelly device", ex);
        }
        return new ShellyException(device.getId(), device.getIp(), "Shelly operation failed", ex);
    }

    private String statusUrl(SocketDevice device) {
        return String.format("http://%s/rpc/Switch.Get?id=%d", device.getIp(), device.getRelayIndex());
    }

    private String switchUrl(SocketDevice device, boolean turnOn) {
        return String.format("http://%s/rpc/Switch.Set?id=%d&on=%s", device.getIp(), device.getRelayIndex(), turnOn);
    }
}
