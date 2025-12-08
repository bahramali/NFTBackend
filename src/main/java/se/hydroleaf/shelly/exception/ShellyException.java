package se.hydroleaf.shelly.exception;

public class ShellyException extends RuntimeException {

    private final String deviceId;
    private final String ip;

    public ShellyException(String deviceId, String ip, String message, Throwable cause) {
        super(message, cause);
        this.deviceId = deviceId;
        this.ip = ip;
    }

    public ShellyException(String deviceId, String ip, String message) {
        super(message);
        this.deviceId = deviceId;
        this.ip = ip;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getIp() {
        return ip;
    }
}
