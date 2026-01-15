package se.hydroleaf.store.service;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.hydroleaf.common.api.BadRequestException;
import se.hydroleaf.common.api.ConflictException;
import se.hydroleaf.common.api.NotFoundException;
import se.hydroleaf.common.api.StripeIntegrationException;
import se.hydroleaf.store.api.dto.CheckoutRequest;
import se.hydroleaf.store.api.dto.CheckoutResponse;
import se.hydroleaf.store.api.dto.ShippingAddressDto;
import se.hydroleaf.store.config.StoreProperties;
import se.hydroleaf.store.model.Cart;
import se.hydroleaf.store.model.CartItem;
import se.hydroleaf.store.model.CartStatus;
import se.hydroleaf.store.model.OrderItem;
import se.hydroleaf.store.model.OrderStatus;
import se.hydroleaf.store.model.Payment;
import se.hydroleaf.store.model.PaymentProvider;
import se.hydroleaf.store.model.PaymentStatus;
import se.hydroleaf.store.model.Product;
import se.hydroleaf.store.model.ProductVariant;
import se.hydroleaf.store.model.ShippingAddress;
import se.hydroleaf.store.model.StoreOrder;
import se.hydroleaf.store.repository.CartRepository;
import se.hydroleaf.store.repository.OrderRepository;
import se.hydroleaf.store.repository.PaymentRepository;
import se.hydroleaf.store.repository.ProductVariantRepository;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private final CartRepository cartRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final StoreProperties storeProperties;
    private final StripeService stripeService;

    @Transactional
    public CheckoutResponse checkout(CheckoutRequest request) {
        Cart cart = cartRepository.findLockedWithItems(request.getCartId())
                .orElseThrow(() -> new NotFoundException("CART_NOT_FOUND", "Cart not found"));
        if (cart.getStatus() != CartStatus.OPEN) {
            throw new ConflictException("CART_CLOSED", "Cart is no longer open");
        }
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("EMPTY_CART", "Cart has no items");
        }

        if (request.getUserId() != null && cart.getUserId() == null) {
            cart.setUserId(request.getUserId());
        }

        PricingTotals totals = repriceAndReserveInventory(cart);
        StoreOrder order = buildOrder(cart, request, totals);
        orderRepository.save(order);

        String paymentUrl = storeProperties.getFallbackPaymentUrl().replace("{orderId}", order.getId().toString());
        String providerRef = order.getOrderNumber();

        try {
            StripeService.StripeSessionResult sessionResult = stripeService.createCheckoutSession(order);
            if (sessionResult != null) {
                paymentUrl = sessionResult.url();
                providerRef = sessionResult.sessionId();
            }
        } catch (StripeIntegrationException ex) {
            log.error("Stripe session creation failed for orderId={}: {}", order.getId(), ex.getMessage());
            throw ex;
        }

        Payment payment = Payment.builder()
                .order(order)
                .provider(PaymentProvider.STRIPE)
                .status(PaymentStatus.CREATED)
                .amountCents(order.getTotalCents())
                .currency(order.getCurrency())
                .providerPaymentId(providerRef)
                .providerReference(providerRef)
                .build();
        paymentRepository.save(payment);

        // cart closes only on Stripe webhook confirmation.
        cartRepository.save(cart);

        log.info("Checkout initiated cartId={} orderId={}", cart.getId(), order.getId());
        return CheckoutResponse.builder()
                .orderId(order.getId())
                .paymentUrl(paymentUrl)
                .build();
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
        cart.setUpdatedAt(Instant.now());
        return new PricingTotals(subtotal, shipping, tax, total);
    }

    private StoreOrder buildOrder(Cart cart, CheckoutRequest request, PricingTotals totals) {
        UUID userId = request.getUserId() != null ? request.getUserId() : cart.getUserId();
        StoreOrder order = StoreOrder.builder()
                .orderNumber(generateOrderNumber())
                .userId(userId)
                .email(request.getEmail())
                .status(OrderStatus.OPEN)
                .subtotalCents(totals.subtotal())
                .shippingCents(totals.shipping())
                .taxCents(totals.tax())
                .totalCents(totals.total())
                .totalAmountCents(totals.total())
                .currency(storeProperties.getCurrency())
                .shippingAddress(toShippingAddress(request.getShippingAddress()))
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

    private ShippingAddress toShippingAddress(ShippingAddressDto dto) {
        return ShippingAddress.builder()
                .name(dto.getName())
                .line1(dto.getLine1())
                .line2(dto.getLine2())
                .city(dto.getCity())
                .state(dto.getState())
                .postalCode(dto.getPostalCode())
                .country(dto.getCountry())
                .phone(dto.getPhone())
                .build();
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

    private record PricingTotals(long subtotal, long shipping, long tax, long total) {}
}
