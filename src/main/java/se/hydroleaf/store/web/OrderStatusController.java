package se.hydroleaf.store.web;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.hydroleaf.common.api.NotFoundException;
import se.hydroleaf.store.api.dto.OrderStatusResponse;
import se.hydroleaf.store.api.dto.orders.v1.OrderStatusMapper;
import se.hydroleaf.store.model.Payment;
import se.hydroleaf.store.model.StoreOrder;
import se.hydroleaf.store.repository.OrderRepository;
import se.hydroleaf.store.repository.PaymentRepository;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderStatusController {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderStatusResponse> getOrderStatus(@PathVariable UUID orderId) {
        StoreOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
        Payment payment = paymentRepository.findTopByOrderIdOrderByUpdatedAtDesc(orderId).orElse(null);
        String paymentStatus = OrderStatusMapper.toPaymentStatus(payment);
        return ResponseEntity.ok(OrderStatusResponse.builder()
                .orderId(order.getId())
                .orderStatus(order.getStatus().name())
                .paymentStatus(paymentStatus)
                .displayStatus(OrderStatusMapper.toDisplayStatus(order.getStatus(), paymentStatus))
                .totalAmountCents(order.getTotalCents())
                .currency(order.getCurrency())
                .build());
    }
}
