package se.hydroleaf.service;

import com.stripe.exception.StripeException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import se.hydroleaf.common.api.StripeIntegrationException;
import se.hydroleaf.controller.dto.MyDeviceMetricResponse;
import se.hydroleaf.controller.dto.MyDeviceResponse;
import se.hydroleaf.store.api.dto.orders.v1.OrderDetailsDTO;
import se.hydroleaf.store.api.dto.orders.v1.OrderSummaryDTO;
import se.hydroleaf.store.api.dto.orders.v1.PaymentActionDTO;
import se.hydroleaf.controller.dto.MyProfileResponse;
import se.hydroleaf.controller.dto.UpdateMyProfileRequest;
import se.hydroleaf.model.Device;
import se.hydroleaf.model.LatestSensorValue;
import se.hydroleaf.model.User;
import se.hydroleaf.payments.stripe.StripeCheckoutService;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.LatestSensorValueRepository;
import se.hydroleaf.repository.UserRepository;
import se.hydroleaf.store.model.OrderStatus;
import se.hydroleaf.store.model.StoreOrder;
import se.hydroleaf.store.repository.OrderRepository;
import se.hydroleaf.store.repository.PaymentRepository;
import se.hydroleaf.store.model.Payment;
import se.hydroleaf.store.model.PaymentProvider;
import se.hydroleaf.store.model.PaymentStatus;

@Service
@RequiredArgsConstructor
public class MyAccountService {

    private static final Duration ONLINE_THRESHOLD = Duration.ofMinutes(5);

    private final AuthorizationService authorizationService;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final LatestSensorValueRepository latestSensorValueRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final StripeCheckoutService stripeCheckoutService;
    private final Clock clock;

    public MyProfileResponse getCurrentProfile(String token) {
        AuthenticatedUser authenticatedUser = authorizationService.requireAuthenticated(token);
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return MyProfileResponse.from(user);
    }

    public MyProfileResponse updateCurrentProfile(AuthenticatedUser authenticatedUser, UpdateMyProfileRequest request) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (request.fullName() != null) {
            user.setDisplayName(request.fullName().trim());
        }
        if (request.phoneNumber() != null) {
            user.setPhone(request.phoneNumber().trim());
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

    @Transactional(readOnly = true)
    public List<OrderSummaryDTO> listMyOrders(String token) {
        AuthenticatedUser authenticatedUser = authorizationService.requireAuthenticated(token);
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        List<StoreOrder> orders = orderRepository.findByEmailIgnoreCase(user.getEmail());
        Map<UUID, OrderSummaryDTO.ItemCounts> itemCounts = loadItemCounts(orders);
        Map<UUID, Payment> paymentsByOrderId = orders.isEmpty()
                ? Map.of()
                : paymentRepository.findByOrderIdIn(orders.stream().map(StoreOrder::getId).toList()).stream()
                        .collect(Collectors.toMap(
                                payment -> payment.getOrder().getId(),
                                Function.identity(),
                                (left, right) -> left.getUpdatedAt().isAfter(right.getUpdatedAt()) ? left : right
                        ));
        return orders.stream()
                .map(order -> {
                    Payment payment = paymentsByOrderId.get(order.getId());
                    PaymentActionDTO paymentAction = resolvePaymentAction(order, payment);
                    OrderSummaryDTO.ItemCounts counts = itemCounts.getOrDefault(
                            order.getId(),
                            new OrderSummaryDTO.ItemCounts(0, 0)
                    );
                    return OrderSummaryDTO.from(order, payment, paymentAction, counts);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDetailsDTO getMyOrder(String token, UUID orderId) {
        AuthenticatedUser authenticatedUser = authorizationService.requireAuthenticated(token);
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        StoreOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order does not belong to user");
        }
        Payment payment = paymentRepository.findTopByOrderIdOrderByUpdatedAtDesc(order.getId()).orElse(null);
        return OrderDetailsDTO.from(order, payment, resolvePaymentAction(order, payment));
    }

    private PaymentActionDTO resolvePaymentAction(StoreOrder order, Payment payment) {
        if (!isPayable(order, payment)) {
            return null;
        }
        if (payment != null && payment.getProvider() != PaymentProvider.STRIPE) {
            return null;
        }
        try {
            String idempotencyKey = buildIdempotencyKey(order, payment);
            String url = stripeCheckoutService.createCheckoutSession(order.getId(), idempotencyKey).url();
            return new PaymentActionDTO("REDIRECT", "Continue payment", url);
        } catch (StripeException ex) {
            throw StripeIntegrationException.fromStripeException(ex);
        }
    }

    private boolean isPayable(StoreOrder order, Payment payment) {
        if (order == null) {
            return false;
        }
        if (order.getStatus() != OrderStatus.OPEN) {
            return false;
        }
        if (payment == null) {
            return true;
        }
        return payment.getStatus() != PaymentStatus.PAID
                && payment.getStatus() != PaymentStatus.CANCELLED
                && payment.getStatus() != PaymentStatus.REFUNDED;
    }

    private String buildIdempotencyKey(StoreOrder order, Payment payment) {
        Instant updatedAt = payment != null ? payment.getUpdatedAt() : order.getCreatedAt();
        return order.getId() + ":" + updatedAt.toEpochMilli();
    }

    private Map<UUID, OrderSummaryDTO.ItemCounts> loadItemCounts(List<StoreOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return Map.of();
        }
        List<UUID> orderIds = orders.stream().map(StoreOrder::getId).toList();
        return orderRepository.findItemCounts(orderIds).stream()
                .collect(Collectors.toMap(
                        OrderRepository.OrderItemCounts::getId,
                        counts -> new OrderSummaryDTO.ItemCounts(
                                Math.toIntExact(counts.getItemsCount()),
                                Math.toIntExact(counts.getItemsQuantity())
                        )
                ));
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
