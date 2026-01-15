package se.hydroleaf.store.service;

import com.stripe.model.Address;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.hydroleaf.common.api.ConflictException;
import se.hydroleaf.common.api.NotFoundException;
import se.hydroleaf.store.config.StoreProperties;
import se.hydroleaf.store.model.Cart;
import se.hydroleaf.store.model.CartItem;
import se.hydroleaf.store.model.CartStatus;
import se.hydroleaf.store.model.OrderItem;
import se.hydroleaf.store.model.OrderStatus;
import se.hydroleaf.store.model.Payment;
import se.hydroleaf.store.model.PaymentAttempt;
import se.hydroleaf.store.model.PaymentAttemptStatus;
import se.hydroleaf.store.model.PaymentProvider;
import se.hydroleaf.store.model.PaymentStatus;
import se.hydroleaf.store.model.Product;
import se.hydroleaf.store.model.ProductVariant;
import se.hydroleaf.store.model.ShippingAddress;
import se.hydroleaf.store.model.StoreOrder;
import se.hydroleaf.store.repository.CartRepository;
import se.hydroleaf.store.repository.OrderRepository;
import se.hydroleaf.store.repository.PaymentAttemptRepository;
import se.hydroleaf.store.repository.PaymentRepository;
import se.hydroleaf.store.repository.ProductVariantRepository;

@Service
@RequiredArgsConstructor
public class StripeWebhookOrderService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookOrderService.class);

    private final CartRepository cartRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final StoreProperties storeProperties;

    @Transactional
    public void finalizePaidOrder(Session session) {
        if (session == null) {
            return;
        }

        String sessionId = session.getId();
        if (!StringUtils.hasText(sessionId)) {
            log.warn("Stripe session missing id");
            return;
        }

        String paymentIntent = session.getPaymentIntent();
        Payment existingPayment = findExistingPayment(sessionId, paymentIntent);
        if (existingPayment != null) {
            if (existingPayment.getStatus() == PaymentStatus.PAID) {
                log.info("Stripe webhook already processed for sessionId={}", sessionId);
                return;
            }
            updatePaymentAsPaid(existingPayment, paymentIntent);
            return;
        }

        UUID cartId = resolveCartId(sessionId, session.getMetadata());
        if (cartId == null) {
            log.warn("Stripe webhook missing cartId sessionId={}", sessionId);
            return;
        }

        Cart cart = cartRepository.findLockedWithItems(cartId).orElse(null);
        if (cart == null) {
            log.warn("Stripe webhook received for unknown cart cartId={} sessionId={}", cartId, sessionId);
            return;
        }
        if (cart.getStatus() == CartStatus.CHECKED_OUT) {
            log.info("Stripe webhook received for already checked out cart cartId={} sessionId={}", cartId, sessionId);
            return;
        }
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            log.warn("Stripe webhook received for empty cart cartId={} sessionId={}", cartId, sessionId);
            return;
        }

        PricingTotals totals = repriceAndReserveInventory(cart);
        String email = resolveEmail(session);
        if (!StringUtils.hasText(email)) {
            log.warn("Stripe webhook missing customer email sessionId={}", sessionId);
            return;
        }

        StoreOrder order = buildOrder(cart, totals, email, resolveShippingAddress(session));
        orderRepository.save(order);

        Payment payment = Payment.builder()
                .order(order)
                .provider(PaymentProvider.STRIPE)
                .status(PaymentStatus.PAID)
                .amountCents(order.getTotalCents())
                .currency(order.getCurrency())
                .providerPaymentId(sessionId)
                .providerReference(StringUtils.hasText(paymentIntent) ? paymentIntent : sessionId)
                .build();
        paymentRepository.save(payment);

        cart.setStatus(CartStatus.CHECKED_OUT);
        cartRepository.save(cart);

        log.info("Created paid order from Stripe webhook orderId={} sessionId={}", order.getId(), sessionId);
    }

    @Transactional
    public void markCheckoutExpired(Session session) {
        if (session == null) {
            return;
        }
        String sessionId = session.getId();
        if (!StringUtils.hasText(sessionId)) {
            log.warn("Stripe session missing id for expiration event");
            return;
        }
        PaymentAttempt attempt = paymentAttemptRepository.findByStripeSessionId(sessionId).orElse(null);
        if (attempt == null) {
            log.warn("Stripe checkout expired without matching payment attempt sessionId={}", sessionId);
            return;
        }
        if (attempt.getStatus() != PaymentAttemptStatus.CANCELLED) {
            attempt.setStatus(PaymentAttemptStatus.CANCELLED);
            paymentAttemptRepository.save(attempt);
        }
        log.info("Marked Stripe checkout expired for cartId={} sessionId={}", attempt.getCartId(), sessionId);
    }

    @Transactional
    public void markPaymentFailed(PaymentIntent paymentIntent) {
        if (paymentIntent == null) {
            return;
        }
        String cartIdValue = paymentIntent.getMetadata() != null ? paymentIntent.getMetadata().get("cartId") : null;
        if (!StringUtils.hasText(cartIdValue)) {
            log.warn("Stripe payment failed missing cartId metadata paymentIntent={}", paymentIntent.getId());
            return;
        }
        UUID cartId;
        try {
            cartId = UUID.fromString(cartIdValue);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid cartId metadata on Stripe payment failed paymentIntent={}", paymentIntent.getId());
            return;
        }
        PaymentAttempt attempt = paymentAttemptRepository.findTopByCartIdOrderByCreatedAtDesc(cartId).orElse(null);
        if (attempt == null) {
            log.warn("Stripe payment failed without matching payment attempt cartId={} paymentIntent={}", cartId, paymentIntent.getId());
            return;
        }
        if (attempt.getStatus() != PaymentAttemptStatus.FAILED) {
            attempt.setStatus(PaymentAttemptStatus.FAILED);
            paymentAttemptRepository.save(attempt);
        }
        log.info("Marked Stripe payment failed for cartId={} paymentIntent={}", cartId, paymentIntent.getId());
    }

    private Payment findExistingPayment(String sessionId, String paymentIntent) {
        if (StringUtils.hasText(paymentIntent)) {
            Payment byIntent = paymentRepository.findByProviderReference(paymentIntent).orElse(null);
            if (byIntent != null) {
                return byIntent;
            }
        }
        return paymentRepository.findByProviderPaymentId(sessionId).orElse(null);
    }

    private void updatePaymentAsPaid(Payment payment, String paymentIntent) {
        payment.setStatus(PaymentStatus.PAID);
        if (StringUtils.hasText(paymentIntent)) {
            payment.setProviderReference(paymentIntent);
        }
        StoreOrder order = payment.getOrder();
        if (order.getStatus() != OrderStatus.PAID) {
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
        }
        paymentRepository.save(payment);
        log.info("Marked existing order paid from Stripe webhook orderId={} paymentId={}",
                order.getId(), payment.getProviderPaymentId());
    }

    private UUID resolveCartId(String sessionId, Map<String, String> metadata) {
        PaymentAttempt attempt = paymentAttemptRepository.findByStripeSessionId(sessionId).orElse(null);
        if (attempt != null) {
            return attempt.getCartId();
        }
        if (metadata != null && StringUtils.hasText(metadata.get("cartId"))) {
            try {
                return UUID.fromString(metadata.get("cartId"));
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid cartId metadata in Stripe session {}", sessionId);
            }
        }
        return null;
    }

    private PricingTotals repriceAndReserveInventory(Cart cart) {
        long subtotal = 0L;
        List<ProductVariant> updatedVariants = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            if (item.getVariant() == null) {
                throw new NotFoundException("VARIANT_NOT_FOUND", "Product variant not available");
            }
            ProductVariant variant = productVariantRepository.findWithLock(item.getVariant().getId())
                    .orElseThrow(() -> new NotFoundException("VARIANT_NOT_FOUND", "Product variant not available"));
            Product product = variant.getProduct();
            if (!product.isActive()) {
                throw new ConflictException("PRODUCT_INACTIVE", "Product " + product.getName() + " is no longer available");
            }
            if (!variant.isActive()) {
                throw new ConflictException("VARIANT_INACTIVE", "Variant " + variant.getLabel() + " is no longer available");
            }
            if (item.getQty() > variant.getStockQuantity()) {
                throw new ConflictException("INSUFFICIENT_STOCK", "Not enough stock for " + variant.getLabel());
            }
            if (!storeProperties.getCurrency().equalsIgnoreCase(product.getCurrency())) {
                throw new ConflictException("CURRENCY_MISMATCH", "Product currency mismatch for " + product.getName());
            }

            item.setProduct(product);
            item.setVariant(variant);
            item.setUnitPriceCents(variant.getPriceCents());
            item.setLineTotalCents(Math.multiplyExact(variant.getPriceCents(), (long) item.getQty()));
            subtotal += item.getLineTotalCents();

            variant.setStockQuantity(variant.getStockQuantity() - item.getQty());
            updatedVariants.add(variant);
        }

        productVariantRepository.saveAll(updatedVariants);
        long shipping = storeProperties.getShippingFlatCents();
        long tax = calculateTax(subtotal);
        long total = subtotal + shipping + tax;
        return new PricingTotals(subtotal, shipping, tax, total);
    }

    private StoreOrder buildOrder(Cart cart, PricingTotals totals, String email, ShippingAddress shippingAddress) {
        StoreOrder order = StoreOrder.builder()
                .orderNumber(generateOrderNumber())
                .userId(cart.getUserId())
                .email(email)
                .status(OrderStatus.PAID)
                .subtotalCents(totals.subtotal())
                .shippingCents(totals.shipping())
                .taxCents(totals.tax())
                .totalCents(totals.total())
                .totalAmountCents(totals.total())
                .currency(storeProperties.getCurrency())
                .shippingAddress(shippingAddress)
                .items(new ArrayList<>())
                .build();

        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(cartItem.getProduct())
                    .variant(cartItem.getVariant())
                    .nameSnapshot(cartItem.getProduct().getName())
                    .unitPriceCents(cartItem.getUnitPriceCents())
                    .qty(cartItem.getQty())
                    .lineTotalCents(cartItem.getLineTotalCents())
                    .build();
            order.getItems().add(orderItem);
        }
        return order;
    }

    private long calculateTax(long subtotal) {
        BigDecimal rate = storeProperties.getTaxRatePercent();
        if (rate == null || rate.signum() == 0) {
            return 0L;
        }
        return rate.multiply(BigDecimal.valueOf(subtotal))
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP)
                .longValue();
    }

    private String generateOrderNumber() {
        return "HL-" + Instant.now().toEpochMilli();
    }

    private String resolveEmail(Session session) {
        Session.CustomerDetails customerDetails = session.getCustomerDetails();
        if (customerDetails != null && StringUtils.hasText(customerDetails.getEmail())) {
            return customerDetails.getEmail();
        }
        if (StringUtils.hasText(session.getCustomerEmail())) {
            return session.getCustomerEmail();
        }
        return null;
    }

    private ShippingAddress resolveShippingAddress(Session session) {
        Session.CustomerDetails customerDetails = session.getCustomerDetails();
        if (customerDetails != null && customerDetails.getAddress() != null) {
            Address address = customerDetails.getAddress();
            return ShippingAddress.builder()
                    .name(customerDetails.getName())
                    .line1(address.getLine1())
                    .line2(address.getLine2())
                    .city(address.getCity())
                    .state(address.getState())
                    .postalCode(address.getPostalCode())
                    .country(address.getCountry())
                    .phone(customerDetails.getPhone())
                    .build();
        }

        return resolveShippingAddressFromMetadata(session.getMetadata());
    }

    private ShippingAddress resolveShippingAddressFromMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        String name = emptyToNull(metadata.get("ship_name"));
        String line1 = emptyToNull(metadata.get("ship_line1"));
        String line2 = emptyToNull(metadata.get("ship_line2"));
        String postalCode = emptyToNull(metadata.get("ship_postalCode"));
        String city = emptyToNull(metadata.get("ship_city"));
        String state = emptyToNull(metadata.get("ship_state"));
        String country = emptyToNull(metadata.get("ship_country"));
        String phone = emptyToNull(metadata.get("ship_phone"));
        if (!StringUtils.hasText(name)
                && !StringUtils.hasText(line1)
                && !StringUtils.hasText(line2)
                && !StringUtils.hasText(postalCode)
                && !StringUtils.hasText(city)
                && !StringUtils.hasText(state)
                && !StringUtils.hasText(country)
                && !StringUtils.hasText(phone)) {
            return null;
        }
        return ShippingAddress.builder()
                .name(name)
                .line1(line1)
                .line2(line2)
                .city(city)
                .state(state)
                .postalCode(postalCode)
                .country(country)
                .phone(phone)
                .build();
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private record PricingTotals(long subtotal, long shipping, long tax, long total) {}
}
