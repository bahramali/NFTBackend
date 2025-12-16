package se.hydroleaf.shelly.service;

import se.hydroleaf.shelly.dto.SocketStatusDTO;
import se.hydroleaf.shelly.model.SocketDevice;

public interface ShellyClient {
    SocketStatusDTO getStatus(SocketDevice device);

    SocketStatusDTO turnOn(SocketDevice device);

    SocketStatusDTO turnOff(SocketDevice device);

    SocketStatusDTO toggle(SocketDevice device);
}
