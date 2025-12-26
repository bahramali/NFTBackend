package se.hydroleaf.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.controller.dto.MyDeviceMetricResponse;
import se.hydroleaf.controller.dto.MyDeviceResponse;
import se.hydroleaf.controller.dto.MyOrderResponse;
import se.hydroleaf.controller.dto.MyProfileRequest;
import se.hydroleaf.controller.dto.MyProfileResponse;
import se.hydroleaf.model.Device;
import se.hydroleaf.model.LatestSensorValue;
import se.hydroleaf.model.User;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.LatestSensorValueRepository;
import se.hydroleaf.repository.UserRepository;
import se.hydroleaf.store.model.StoreOrder;
import se.hydroleaf.store.repository.OrderRepository;

@Service
@RequiredArgsConstructor
public class MyAccountService {

    private static final Duration ONLINE_THRESHOLD = Duration.ofMinutes(5);

    private final AuthorizationService authorizationService;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final LatestSensorValueRepository latestSensorValueRepository;
    private final OrderRepository orderRepository;
    private final Clock clock;

    public MyProfileResponse getCurrentProfile(String token) {
        AuthenticatedUser authenticatedUser = authorizationService.requireAuthenticated(token);
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return MyProfileResponse.from(user);
    }

    public MyProfileResponse updateCurrentProfile(String token, MyProfileRequest request) {
        AuthenticatedUser authenticatedUser = authorizationService.requireAuthenticated(token);
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (request.fullName() != null) {
            user.setDisplayName(request.fullName().trim());
        }
        if (request.phoneNumber() != null) {
            user.setPhone(request.phoneNumber().trim());
        }
        if (request.notificationPreferences() != null) {
            if (request.notificationPreferences().orderConfirmationEmails() != null) {
                user.setOrderConfirmationEmails(request.notificationPreferences().orderConfirmationEmails());
            }
            if (request.notificationPreferences().pickupReadyNotification() != null) {
                user.setPickupReadyNotification(request.notificationPreferences().pickupReadyNotification());
            }
        }
        if (request.orderConfirmationEmails() != null) {
            user.setOrderConfirmationEmails(request.orderConfirmationEmails());
        }
        if (request.pickupReadyNotification() != null) {
            user.setPickupReadyNotification(request.pickupReadyNotification());
        }
        userRepository.save(user);
        return MyProfileResponse.from(user);
    }

    public List<MyDeviceResponse> listMyDevices(String token) {
        AuthenticatedUser authenticatedUser = authorizationService.requireAuthenticated(token);
        authorizationService.requireAdminOrOperator(authenticatedUser);
        return deviceRepository.findByOwnerUserId(authenticatedUser.userId()).stream()
                .map(this::toDeviceResponse)
                .toList();
    }

    public MyDeviceResponse getMyDevice(String token, String deviceId) {
        AuthenticatedUser authenticatedUser = authorizationService.requireAuthenticated(token);
        authorizationService.requireAdminOrOperator(authenticatedUser);
        Device device = deviceRepository.findByOwnerUserIdAndDeviceId(authenticatedUser.userId(), deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        return toDeviceResponse(device);
    }

    public List<MyOrderResponse> listMyOrders(String token) {
        AuthenticatedUser authenticatedUser = authorizationService.requireAuthenticated(token);
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return orderRepository.findByEmailIgnoreCase(user.getEmail()).stream()
                .map(MyOrderResponse::from)
                .toList();
    }

    public MyOrderResponse getMyOrder(String token, UUID orderId) {
        AuthenticatedUser authenticatedUser = authorizationService.requireAuthenticated(token);
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        StoreOrder order = orderRepository.findById(orderId)
                .filter(o -> o.getEmail().equalsIgnoreCase(user.getEmail()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return MyOrderResponse.from(order);
    }

    private MyDeviceResponse toDeviceResponse(Device device) {
        List<LatestSensorValue> latest = latestSensorValueRepository.findByDevice_CompositeId(device.getCompositeId());
        Optional<Instant> lastSeen = latest.stream()
                .map(LatestSensorValue::getValueTime)
                .filter(t -> t != null)
                .max(Comparator.naturalOrder());

        Instant now = clock.instant();
        boolean online = lastSeen
                .map(ls -> !ls.isBefore(now.minus(ONLINE_THRESHOLD)))
                .orElse(false);

        List<MyDeviceMetricResponse> metrics = latest.stream()
                .sorted(Comparator.comparing(LatestSensorValue::getValueTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(lsv -> new MyDeviceMetricResponse(
                        lsv.getSensorType(),
                        lsv.getValue(),
                        lsv.getUnit(),
                        lsv.getValueTime()
                ))
                .toList();

        return new MyDeviceResponse(
                device.getDeviceId(),
                device.getName(),
                online,
                lastSeen.orElse(null),
                metrics
        );
    }
}
