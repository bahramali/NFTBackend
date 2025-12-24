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
import se.hydroleaf.store.model.StoreOrder;
import se.hydroleaf.store.repository.OrderRepository;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderStatusController {

    private final OrderRepository orderRepository;

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderStatusResponse> getOrderStatus(@PathVariable UUID orderId) {
        StoreOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
        return ResponseEntity.ok(OrderStatusResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus().name())
                .totalAmountCents(order.getTotalCents())
                .currency(order.getCurrency())
                .build());
    }
}
